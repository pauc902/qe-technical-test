package com.example.qe.api.model;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds a valid Pet with sensible defaults, so a test only states the fields it cares about.
 * The id is random: the public Petstore is shared by everyone, so a fixed id could already exist
 * or be changed by someone else between our create and our read.
 */
public final class PetBuilder {

    private Long id = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
    private String name = "qe-test-pet-" + id;
    private String status = "available";

    private PetBuilder() {
    }

    public static PetBuilder aPet() {
        return new PetBuilder();
    }

    public PetBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PetBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public PetBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    public Pet build() {
        return new Pet(id, new Category(1L, "Dogs"), name,
                List.of("https://example.com/photo.jpg"), List.of(new Tag(1L, "automation")), status);
    }
}
