package com.aviation.airport.feign;

import com.aviation.airport.feign.dto.AviationApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign declarative HTTP client for the Aviation API v2.
 *
 * <p>The {@code name} attribute is used by Resilience4j to correlate circuit-breaker
 * and retry instances (see application.yml → resilience4j.circuitbreaker.instances).
 *
 * <p>The {@code url} is externalised via {@code ${aviation.api.base-url}} so the
 * base address can be overridden per-environment without a code change.
 *
 * <p><b>Design note – Provider abstraction:</b> This interface lives in the
 * {@code feign} package and is deliberately kept internal to the infrastructure
 * layer. The {@link com.aviation.airport.service.AirportService} depends on the
 * {@link AviationApiProvider} port, not on this Feign client directly, making it
 * straightforward to swap the upstream provider (e.g. OurAirports, AviationStack)
 * without touching service or controller code.
 */
@FeignClient(
        name = "aviation-api-client",
        url = "${aviation.api.base-url}"
)
public interface AviationApiClient {

    /**
     * Fetches airport data and navigation charts for the given ICAO identifier.
     *
     * @param airport the 4-letter ICAO code (e.g. {@code KJFK})
     * @param airac   AIRAC cycle number; {@code 0} means the current cycle
     * @return raw response from the upstream API
     */
    @GetMapping("/v2/charts")
    AviationApiResponse getAirportCharts(
            @RequestParam("airport") String airport,
            @RequestParam("airac") int airac
    );
}
