package com.example.qe.api.client;

import com.example.qe.api.model.Pet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Thin client for the Petstore /pet endpoints. Tests call createPet() and getPet() and never build
 * URLs or headers themselves. It does NOT check anything: the tests own the expectations.
 */
public class PetApiClient {

    private final APIRequestContext request;

    public PetApiClient(APIRequestContext request) {
        this.request = request;
    }

    public ApiResult createPet(Pet pet) {
        return snapshot(request.post("pet", RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(toJson(pet))));
    }

    /** The id is a String so tests can also send invalid ids such as "abc". */
    public ApiResult getPet(String id) {
        return snapshot(request.get("pet/" + id));
    }

    public ApiResult getPet(long id) {
        return getPet(String.valueOf(id));
    }

    public ApiResult deletePet(long id) {
        return snapshot(request.delete("pet/" + id));
    }

    private static ApiResult snapshot(APIResponse response) {
        return new ApiResult(response.status(), response.text());
    }

    private static String toJson(Object body) {
        try {
            return ApiResult.MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
