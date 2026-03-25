package com.aviation.airport.exception;

import com.aviation.airport.dto.ErrorResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception-to-HTTP mapping.
 *
 * <p>All error responses are returned as {@link ErrorResponse} JSON, giving API
 * consumers a consistent envelope to parse regardless of the error type.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidIcaoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidIcao(InvalidIcaoException ex, HttpServletRequest req) {
        log.warn("Invalid ICAO: {}", ex.getMessage());
        return ErrorResponse.of(400, "BAD_REQUEST", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(AirportNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(AirportNotFoundException ex, HttpServletRequest req) {
        log.warn("Airport not found: {}", ex.getMessage());
        return ErrorResponse.of(404, "NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleUpstreamUnavailable(UpstreamUnavailableException ex, HttpServletRequest req) {
        log.error("Upstream unavailable: {}", ex.getMessage());
        return ErrorResponse.of(503, "SERVICE_UNAVAILABLE", ex.getMessage(), req.getRequestURI());
    }

    /**
     * Catch-all for Feign errors that might escape the service layer.
     */
    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleFeignException(FeignException ex, HttpServletRequest req) {
        log.error("Unhandled Feign error: HTTP {} – {}", ex.status(), ex.getMessage());
        return ErrorResponse.of(502, "BAD_GATEWAY",
                "Error communicating with upstream aviation API.", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return ErrorResponse.of(500, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.", req.getRequestURI());
    }
}
