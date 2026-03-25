package com.aviation.airport.exception;

/**
 * Thrown when the upstream API returns a successful response but contains
 * no airport data for the given ICAO code (e.g. unknown / non-existent code).
 */
public class AirportNotFoundException extends RuntimeException {
    public AirportNotFoundException(String icao) {
        super("Airport not found for ICAO: " + icao);
    }
}
