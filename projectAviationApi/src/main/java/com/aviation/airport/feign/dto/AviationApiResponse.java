package com.aviation.airport.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AviationApiResponse(
        @JsonProperty("airport_data") AirportData airportData,
        Charts charts
) {

    public record AirportData(
            String city,
            @JsonProperty("state_abbr") String stateAbbr,
            @JsonProperty("state_full") String stateFull,
            String country,
            @JsonProperty("icao_ident") String icaoIdent,
            @JsonProperty("faa_ident") String faaIdent,
            @JsonProperty("airport_name") String airportName,
            @JsonProperty("is_military") boolean isMilitary
    ) {}

    public record Charts(
            @JsonProperty("airport_diagram") List<Chart> airportDiagram,
            List<Chart> general,
            List<Chart> departure,
            List<Chart> arrival,
            List<Chart> approach
    ) {}

    public record Chart(
            @JsonProperty("chart_name")     String chartName,
            @JsonProperty("chart_sequence") String chartSequence,
            @JsonProperty("pdf_name")       String pdfName,
            @JsonProperty("pdf_url")        String pdfUrl,
            @JsonProperty("did_change")     boolean didChange,
            // optional fields present only when didChange == true
            @JsonProperty("change_pdf_name") String changePdfName,
            @JsonProperty("change_pdf_url")  String changePdfUrl
    ) {}
}
