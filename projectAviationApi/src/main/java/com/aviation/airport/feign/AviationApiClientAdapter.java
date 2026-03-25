package com.aviation.airport.feign;

import com.aviation.airport.feign.dto.AviationApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter that bridges the {@link AviationApiProvider} port to the
 * concrete {@link AviationApiClient} Feign client.
 *
 * <h3>Resilience strategy (applied in this order):</h3>
 * <ol>
 *   <li><b>RateLimiter</b> – prevents this service from hammering the upstream API
 *       (configured limit: 20 req/s in application.yml).</li>
 *   <li><b>Retry</b> – up to 3 attempts with exponential back-off (500ms → 1s → 2s)
 *       for transient errors (IO, timeout, 503, 504).  4xx errors are NOT retried.</li>
 *   <li><b>CircuitBreaker</b> – opens after 50% failure rate in a 10-call window;
 *       waits 30 s before probing again (HALF_OPEN).</li>
 * </ol>
 *
 * <p>The {@code fallbackMethod} on the circuit breaker returns {@code null} so the
 * service layer can detect the outage and surface a proper 503 to the caller.
 * An alternative design would be to return a cached/stale response here; that
 * trade-off is documented in {@code README.md}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AviationApiClientAdapter implements AviationApiProvider {

    private final AviationApiClient client;

    @Value("${aviation.api.default-airac:0}")
    private int defaultAirac;

    @Override
    @RateLimiter(name = "aviation-api")
    @Retry(name = "aviation-api")
    @CircuitBreaker(name = "aviation-api", fallbackMethod = "fallback")
    public AviationApiResponse fetchByIcao(String icao) {
        log.debug("Calling Aviation API for ICAO={}", icao);
        AviationApiResponse response = client.getAirportCharts(icao, defaultAirac);
        log.debug("Received Aviation API response for ICAO={}", icao);
        return response;
    }

    /**
     * Circuit-breaker fallback: called when the CB is OPEN or when all retries
     * are exhausted.  Returns {@code null} so the service can propagate a
     * meaningful HTTP 503 to the consumer.
     */
    @SuppressWarnings("unused")
    private AviationApiResponse fallback(String icao, Throwable cause) {
        log.error("Circuit breaker OPEN or retries exhausted for ICAO={}. Cause: {}",
                icao, cause.getMessage());
        return null;
    }
}
