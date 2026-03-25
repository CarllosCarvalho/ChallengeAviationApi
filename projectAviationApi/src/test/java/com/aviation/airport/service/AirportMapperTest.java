package com.aviation.airport.service;

import com.aviation.airport.dto.AirportDetailsResponse;
import com.aviation.airport.feign.dto.AviationApiResponse;
import com.aviation.airport.mapper.AirportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AirportMapperTest {

    private final AirportMapper mapper = new AirportMapper();

    @Test
    @DisplayName("Should map all airport data fields correctly")
    void mapsAirportDataFields() {
        var raw = buildResponse();
        AirportDetailsResponse result = mapper.toResponse(raw);

        assertThat(result.icao()).isEqualTo("KJFK");
        assertThat(result.iata()).isEqualTo("JFK");
        assertThat(result.city()).isEqualTo("New York");         // title-cased
        assertThat(result.name()).isEqualTo("John F Kennedy Intl"); // title-cased
        assertThat(result.state()).isEqualTo("NY");
        assertThat(result.country()).isEqualTo("USA");
        assertThat(result.military()).isFalse();
    }

    @Test
    @DisplayName("Should map approach chart with change URL when didChange=true")
    void mapsChangedChart() {
        var raw = buildResponse();
        AirportDetailsResponse result = mapper.toResponse(raw);

        var approach = result.charts().approach().get(0);
        assertThat(approach.name()).isEqualTo("ILS OR LOC RWY 04L");
        assertThat(approach.changed()).isFalse();
        assertThat(approach.changePdfUrl()).isNull();

        var departure = result.charts().departure().get(0);
        assertThat(departure.changed()).isTrue();
        assertThat(departure.changePdfUrl()).isEqualTo("https://example.com/changed.pdf");
    }

    @Test
    @DisplayName("Should handle null chart lists gracefully")
    void handlesNullChartLists() {
        var raw = new AviationApiResponse(
                new AviationApiResponse.AirportData("CITY", "ST", "STATE", "US",
                        "KABC", "ABC", "TEST AIRPORT", false),
                new AviationApiResponse.Charts(null, null, null, null, null)
        );

        AirportDetailsResponse result = mapper.toResponse(raw);
        assertThat(result.charts().approach()).isEmpty();
        assertThat(result.charts().departure()).isEmpty();
    }

    private AviationApiResponse buildResponse() {
        var data = new AviationApiResponse.AirportData(
                "NEW YORK", "NY", "NEW YORK", "USA",
                "KJFK", "JFK", "JOHN F KENNEDY INTL", false);

        var approach = new AviationApiResponse.Chart(
                "ILS OR LOC RWY 04L", "50750", "00610il4l.pdf",
                "https://example.com/il4l.pdf", false, null, null);

        var departure = new AviationApiResponse.Chart(
                "DEEZZ SIX", "90100", "deezz.pdf",
                "https://example.com/deezz.pdf", true,
                "deezz_cmp.pdf", "https://example.com/changed.pdf");

        var charts = new AviationApiResponse.Charts(
                List.of(), List.of(), List.of(departure), List.of(), List.of(approach));

        return new AviationApiResponse(data, charts);
    }
}
