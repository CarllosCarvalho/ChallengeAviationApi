package com.aviation.airport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Uniform error envelope returned for all non-2xx responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        int status,

        String error,

        String message,

        String path,

        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now());
    }
}
