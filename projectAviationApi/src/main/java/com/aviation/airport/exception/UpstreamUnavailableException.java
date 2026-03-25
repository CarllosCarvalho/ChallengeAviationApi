package com.aviation.airport.exception;

/**
 * Thrown when the upstream Aviation API is unreachable or the circuit breaker
 * is OPEN and no cached fallback is available.
 */
public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
