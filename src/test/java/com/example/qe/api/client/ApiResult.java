package com.example.qe.api.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A plain copy of an HTTP response: the status code and the body text.
 * Tests assert on this, and a failure message can print the whole response.
 */
public record ApiResult(int status, String body) {

    // Jackson's converter between JSON text and Java objects. Shared, because creating one is costly.
    static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Converts the JSON body into the given Java type, e.g. result.as(Pet.class). */
    public <T> T as(Class<T> type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Response is not a valid " + type.getSimpleName() + ": " + this, e);
        }
    }

    @Override
    public String toString() {
        return "HTTP " + status + " " + body;
    }
}
