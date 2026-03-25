package com.aviation.airport.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the full request pipeline.
 *
 * <p>WireMock intercepts the outbound Feign call so we test real serialisation,
 * Resilience4j wiring and exception handling without hitting the live API.
 *
 * <p>{@code wiremock.server.port=0} assigns a random port; the base-url is
 * overridden in {@code @TestPropertySource} to point at the WireMock server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "aviation.api.base-url=http://localhost:${wiremock.server.port}"
})
class AirportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String KJFK_RESPONSE = """
            {
              "airport_data": {
                "city": "NEW YORK",
                "state_abbr": "NY",
                "state_full": "NEW YORK",
                "country": "USA",
                "icao_ident": "KJFK",
                "faa_ident": "JFK",
                "airport_name": "JOHN F KENNEDY INTL",
                "is_military": false
              },
              "charts": {
                "airport_diagram": [],
                "general": [],
                "departure": [],
                "arrival": [],
                "approach": [
                  {
                    "chart_name": "ILS OR LOC RWY 04L",
                    "chart_sequence": "50750",
                    "pdf_name": "00610il4l.pdf",
                    "pdf_url": "https://charts-v2.aviationapi.com/260319/00610il4l.pdf",
                    "did_change": false
                  }
                ]
              }
            }
            """;

    @BeforeEach
    void resetWireMock() {
        WireMock.reset();
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/airports/KJFK → 200 with airport details")
    void getAirport_success() throws Exception {
        stubFor(get(urlPathEqualTo("/v2/charts"))
                .withQueryParam("airport", equalTo("KJFK"))
                .withQueryParam("airac", equalTo("0"))

                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(KJFK_RESPONSE)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airports/KJFK").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icao").value("KJFK"))
                .andExpect(jsonPath("$.iata").value("JFK"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.military").value(false))
                .andExpect(jsonPath("$.charts.approach[0].name").value("ILS OR LOC RWY 04L"));
    }

    @Test
    @DisplayName("GET /api/v1/airports/kjfk → 200 (case-insensitive)")
    void getAirport_caseInsensitive() throws Exception {
        stubFor(get(urlPathEqualTo("/v2/charts"))
                .withQueryParam("airport", equalTo("KJFK"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(KJFK_RESPONSE)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airports/kjfk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icao").value("KJFK"));
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/airports/TOOLONG → 400 invalid ICAO")
    void getAirport_invalidIcao() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airports/TOOLONG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ── upstream errors ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/airports/SBGR → 503 when upstream is down")
    void getAirport_upstreamDown() throws Exception {
        stubFor(get(urlPathEqualTo("/v2/charts"))
                .willReturn(aResponse().withStatus(503)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airports/SBGR"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /api/v1/airports/SBGR → 503 on upstream timeout")
    void getAirport_timeout() throws Exception {
        stubFor(get(urlPathEqualTo("/v2/charts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10_000)   // exceeds read-timeout
                        .withBody(KJFK_RESPONSE)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airports/SBGR"))
                .andExpect(status().isServiceUnavailable());
    }
}
