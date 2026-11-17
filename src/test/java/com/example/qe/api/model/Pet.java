package com.example.qe.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The Java shape of a Petstore "Pet". Field names match the JSON keys exactly, so Jackson can convert
 * in both directions with no extra configuration.
 * Unknown fields are ignored so that the API adding a new field does not break our tests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pet(Long id, Category category, String name, List<String> photoUrls, List<Tag> tags, String status) {
}
