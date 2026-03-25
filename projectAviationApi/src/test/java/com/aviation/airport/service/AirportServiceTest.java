package com.aviation.airport.service;

import com.aviation.airport.dto.AirportDetailsResponse;
import com.aviation.airport.exception.AirportNotFoundException;
import com.aviation.airport.exception.InvalidIcaoException;
import com.aviation.airport.exception.UpstreamUnavailableException;
import com.aviation.airport.feign.AviationApiProvider;
import com.aviation.airport.feign.dto.AviationApiResponse;
import com.aviation.airport.feign.dto.AviationApiResponse.AirportData;
import com.aviation.airport.mapper.AirportMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AirportServiceTest {

    private AviationApiProvider provider;
    private AirportService service;

    @BeforeEach
    void setUp() {
        provider = mock(AviationApiProvider.class);
        service = new AirportService(provider, new AirportMapper(), new SimpleMeterRegistry());
    }

    // ── validation ────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "K", "KJFKX", "K J F", "KJ!K"})
    @DisplayName("Should reject invalid ICAO codes")
    void invalidIcao_throws(String icao) {
        assertThatThrownBy(() -> service.getAirportDetails(icao))
                .isInstanceOf(InvalidIcaoException.class);
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("Should normalise ICAO to upper-case")
    void normalisesIcaoToUpperCase() {
        var raw = buildRawResponse("KJFK");
        when(provider.fetchByIcao("KJFK")).thenReturn(raw);

        AirportDetailsResponse result = service.getAirportDetails("kjfk");

        assertThat(result.icao()).isEqualTo("KJFK");
        verify(provider).fetchByIcao("KJFK");
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return mapped airport details on success")
    void success_returnsMappedResponse() {
        var raw = buildRawResponse("KJFK");
        when(provider.fetchByIcao("KJFK")).thenReturn(raw);

        AirportDetailsResponse result = service.getAirportDetails("KJFK");

        assertThat(result.icao()).isEqualTo("KJFK");
        assertThat(result.iata()).isEqualTo("JFK");
        assertThat(result.country()).isEqualTo("USA");
    }

    // ── error paths ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw AirportNotFoundException when response has no airport data")
    void emptyAirportData_throwsNotFound() {
        var raw = new AviationApiResponse(null, null);
        when(provider.fetchByIcao("ZZZZ")).thenReturn(raw);

        assertThatThrownBy(() -> service.getAirportDetails("ZZZZ"))
                .isInstanceOf(AirportNotFoundException.class)
                .hasMessageContaining("ZZZZ");
    }

    @Test
    @DisplayName("Should throw UpstreamUnavailableException when provider returns null (CB open)")
    void nullResponse_throwsUpstreamUnavailable() {
        when(provider.fetchByIcao(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.getAirportDetails("SBGR"))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AviationApiResponse buildRawResponse(String icao) {
        var data = new AirportData("NEW YORK", "NY", "NEW YORK", "USA",
                icao, "JFK", "JOHN F KENNEDY INTL", false);
        var charts = new AviationApiResponse.Charts(
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList());
        return new AviationApiResponse(data, charts);
    }
}
