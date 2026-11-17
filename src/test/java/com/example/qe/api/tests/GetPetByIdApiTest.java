package com.example.qe.api.tests;

import com.example.qe.api.client.ApiResult;
import com.example.qe.api.client.PetApiClient;
import com.example.qe.api.model.ApiError;
import com.example.qe.api.model.Pet;
import com.example.qe.reporting.Area;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.qe.api.model.PetBuilder.aPet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Endpoint under test: GET /pet/{petId}
 * POST /pet and DELETE /pet/{petId} are used only for set-up and clean-up, so the test owns its data.
 */
@DisplayName("API: GET /pet/{petId}")
class GetPetByIdApiTest extends ApiTestBase {

    private PetApiClient pets;
    private Pet createdPet;

    @BeforeAll
    void createClient() {
        pets = new PetApiClient(request);
    }

    @AfterEach
    void deleteTestData() {
        if (createdPet != null) {
            pets.deletePet(createdPet.id());
            createdPet = null;
        }
    }

    @Test
    @Tag("positive")
    @Area("Retrieve pet")
    @DisplayName("returns 200 and the full pet record for an existing id")
    void returnsExistingPet() {
        // Set up: create a pet that we own
        Pet expected = aPet().build();
        ApiResult created = pets.createPet(expected);
        assertThat(created.status()).as("set-up failed: %s", created).isEqualTo(200);
        createdPet = expected;

        // Act: ask for it back by id
        ApiResult response = pets.getPet(expected.id());

        // Check: success status, and every field matches what we created
        assertThat(response.status()).as(response.toString()).isEqualTo(200);
        assertThat(response.as(Pet.class)).usingRecursiveComparison().isEqualTo(expected);
    }

    @Tag("negative")
    @Area("Not-found handling")
    @ParameterizedTest(name = "id={0} -> 404 Pet not found")
    @DisplayName("returns 404 'Pet not found' for well-formed ids that do not exist")
    @ValueSource(strings = {"0", "-1", "9223372036854775807" /* the largest valid id */})
    void unknownIdReturnsNotFound(String id) {
        ApiResult response = pets.getPet(id);

        assertThat(response.status()).as(response.toString()).isEqualTo(404);
        ApiError error = response.as(ApiError.class);
        assertThat(error.type()).isEqualTo("error");
        assertThat(error.message()).isEqualTo("Pet not found");
    }

    @Tag("negative")
    @Area("Input validation")
    @ParameterizedTest(name = "id={0} -> rejected as a client error")
    @DisplayName("rejects malformed ids with a 4xx, never a 5xx")
    @ValueSource(strings = {"abc", "1.5", "null", "9223372036854775808" /* one more than the largest id */})
    void malformedIdIsRejectedAsClientError(String id) {
        ApiResult response = pets.getPet(id);

        // Robustness check: bad input from a client must never look like a server fault.
        assertThat(response.status()).as(response.toString()).isBetween(400, 499);
    }

    @Disabled("KNOWN DEFECT: the spec says 400 'Invalid ID supplied', but the API returns 404 and leaks "
            + "a java.lang.NumberFormatException message. Re-enable once it is fixed.")
    @Tag("negative")
    @Tag("known-defect")
    @Area("API contract")
    @ParameterizedTest(name = "id={0} -> 400 per the API contract")
    @DisplayName("returns exactly 400 for malformed ids, without leaking internals")
    @ValueSource(strings = {"abc", "1.5"})
    void malformedIdMatchesContract(String id) {
        ApiResult response = pets.getPet(id);

        assertThat(response.status()).as(response.toString()).isEqualTo(400);
        assertThat(response.body()).doesNotContain("java.lang", "Exception");
    }
}
