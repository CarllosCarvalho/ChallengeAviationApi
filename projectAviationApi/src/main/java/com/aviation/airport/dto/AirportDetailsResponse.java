package com.aviation.airport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Public API response for airport details.
 *
 * <p>This model is intentionally decoupled from the upstream Aviation API contract.
 * Any change to the provider's JSON shape is absorbed by the mapper layer, leaving
 * this contract stable for consumers of <em>this</em> service.
 *
 * <p>{@code @JsonInclude(NON_NULL)} ensures optional chart buckets that happen to
 * be empty are omitted from the response payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AirportDetailsResponse(

        String icao,

        String iata,

        String name,

        String city,

        String state,

        String country,

        boolean military,

        Charts charts

) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Charts(
            List<ChartReference> airportDiagram,

            List<ChartReference> general,

            List<ChartReference> departure,

            List<ChartReference> arrival,

            List<ChartReference> approach
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChartReference(

            String name,

            String pdfUrl,

            boolean changed,

            String changePdfUrl
    ) {}
}
