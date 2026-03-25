package com.aviation.airport.service;

import com.aviation.airport.dto.AirportDetailsResponse;
import com.aviation.airport.exception.AirportNotFoundException;
import com.aviation.airport.exception.InvalidIcaoException;
import com.aviation.airport.exception.UpstreamUnavailableException;
import com.aviation.airport.feign.AviationApiProvider;
import com.aviation.airport.feign.dto.AviationApiResponse;
import com.aviation.airport.mapper.AirportMapper;
import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Core business-logic layer for airport lookups.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Validates the ICAO input format before hitting the network.</li>
 *   <li>Delegates to the {@link AviationApiProvider} abstraction (not the Feign
 *       client directly) to keep this class provider-agnostic.</li>
 *   <li>Translates infrastructure exceptions into domain exceptions that the
 *       controller then maps to HTTP status codes.</li>
 *   <li>Records a {@code airport.lookup} Micrometer timer for latency tracking.</li>
 *   <li>Enriches MDC with the {@code icao} key so all log lines within a request
 *       include the airport identifier automatically.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AirportService {

    /** ICAO codes are always exactly 4 alphanumeric characters. */
    private static final Pattern ICAO_PATTERN = Pattern.compile("^[A-Za-z0-9]{4}$");

    private final AviationApiProvider provider;
    private final AirportMapper mapper;
    private final MeterRegistry meterRegistry;

    /**
     * Fetches and returns airport details for the given ICAO code.
     *
     * @param rawIcao the ICAO code provided by the caller (case-insensitive)
     * @return structured airport details
     * @throws InvalidIcaoException        if the format is invalid
     * @throws AirportNotFoundException    if the upstream returns no data for the code
     * @throws UpstreamUnavailableException if the upstream is unreachable / CB is OPEN
     */
    public AirportDetailsResponse getAirportDetails(String rawIcao) {
        String icao = normaliseAndValidate(rawIcao);

        // Enrich all log lines within this call with the ICAO code
        MDC.put("icao", icao);
        try {
            return Timer.builder("airport.lookup")
                    .description("Time to resolve airport details from upstream")
                    .tag("icao", icao)
                    .register(meterRegistry)
                    .recordCallable(() -> doFetch(icao));
        } catch (AirportNotFoundException | UpstreamUnavailableException e) {
            throw e;  // already a domain exception – let it bubble
        } catch (Exception e) {
            log.error("Unexpected error fetching airport details for ICAO={}", icao, e);
            throw new UpstreamUnavailableException("Unexpected error: " + e.getMessage());
        } finally {
            MDC.remove("icao");
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AirportDetailsResponse doFetch(String icao) {
        AviationApiResponse raw;
        try {
            raw = provider.fetchByIcao(icao);
        } catch (FeignException.NotFound e) {
            log.warn("Upstream returned 404 for ICAO={}", icao);
            throw new AirportNotFoundException(icao);
        } catch (FeignException.TooManyRequests e) {
            log.warn("Upstream rate-limit hit for ICAO={}", icao);
            throw new UpstreamUnavailableException("Upstream rate limit exceeded – try again later.");
        } catch (FeignException e) {
            log.error("Feign error for ICAO={}: HTTP {} – {}", icao, e.status(), e.getMessage());
            throw new UpstreamUnavailableException("Upstream error: " + e.status());
        }

        // Null response = circuit breaker fallback was triggered
        if (raw == null) {
            log.warn("Null response from provider (circuit breaker open?) for ICAO={}", icao);
            throw new UpstreamUnavailableException("Aviation API is currently unavailable.");
        }

        // An empty airport_data block means the code exists but has no data
        if (raw.airportData() == null || raw.airportData().icaoIdent() == null) {
            log.warn("No airport data in upstream response for ICAO={}", icao);
            throw new AirportNotFoundException(icao);
        }

        log.info("Successfully resolved airport '{}' for ICAO={}",
                raw.airportData().airportName(), icao);

        return mapper.toResponse(raw);
    }

    private String normaliseAndValidate(String rawIcao) {
        if (rawIcao == null || rawIcao.isBlank()) {
            throw new InvalidIcaoException(rawIcao);
        }
        String icao = rawIcao.trim().toUpperCase();
        if (!ICAO_PATTERN.matcher(icao).matches()) {
            throw new InvalidIcaoException(icao);
        }
        return icao;
    }
}
