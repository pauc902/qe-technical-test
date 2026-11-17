package com.example.qe.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The Petstore's error body, e.g. {"code":1,"type":"error","message":"Pet not found"}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiError(Integer code, String type, String message) {
}
