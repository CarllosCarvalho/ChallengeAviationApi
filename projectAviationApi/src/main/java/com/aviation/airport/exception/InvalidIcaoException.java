package com.aviation.airport.exception;

/**
 * Thrown when the supplied ICAO code does not pass format validation.
 * This exception is marked as non-retryable in the Resilience4j retry config.
 */
public class InvalidIcaoException extends RuntimeException {
    public InvalidIcaoException(String icao) {
        super("Invalid ICAO code: '" + icao + "'. Must be exactly 4 alphanumeric characters.");
    }
}
