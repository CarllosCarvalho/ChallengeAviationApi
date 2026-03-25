package com.aviation.airport.feign;

import com.aviation.airport.feign.dto.AviationApiResponse;

/**
 * Port (in Hexagonal Architecture terms) that the service layer depends on
 * to retrieve raw airport data.
 *
 * <p>Having this abstraction means the production Feign adapter
 * ({@link AviationApiClientAdapter}) can be swapped for:
 * <ul>
 *   <li>A mock/stub in unit tests</li>
 *   <li>A different provider (OurAirports, AeroDataBox, etc.) without touching
 *       the service or controller layers</li>
 * </ul>
 */
public interface AviationApiProvider {

    /**
     * Retrieves airport data for the given ICAO code using the current AIRAC cycle.
     *
     * @param icao 4-letter ICAO airport identifier
     * @return raw upstream response
     */
    AviationApiResponse fetchByIcao(String icao);
}
