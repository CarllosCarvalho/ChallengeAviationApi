package com.aviation.airport.controller;

import com.aviation.airport.dto.AirportDetailsResponse;
import com.aviation.airport.service.AirportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing airport lookup operations.
 *
 * <p>This class is intentionally thin: it handles HTTP concerns only (routing,
 * status codes, Swagger docs) and delegates all logic to {@link AirportService}.
 *
 * <p>Base path {@code /api/v1} is versioned so future breaking changes can be
 * introduced under {@code /api/v2} without disrupting existing consumers.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Tag(name = "Airports", description = "Airport details and navigation chart lookup by ICAO code")
public class AirportController {

    private final AirportService airportService;

    /**
     * Returns detailed information for the airport identified by the given ICAO code.
     *
     * @param icao 4-letter ICAO identifier (case-insensitive)
     * @return 200 with airport details, or an appropriate error status
     */
    @GetMapping("/{icao}")
    @Operation(
            summary = "Get airport details by ICAO",
            description = """
                    Retrieves airport information (name, location, identifiers) and
                    a structured list of navigation charts (SIDs, STARs, approaches, etc.)
                    for the requested airport.
                    
                    The ICAO code is case-insensitive; it is normalised to upper-case internally.
                    """,
            parameters = @Parameter(
                    name = "icao",
                    description = "4-letter ICAO airport code",
                    example = "KJFK",
                    required = true
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Airport found",
                            content = @Content(schema = @Schema(implementation = AirportDetailsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid ICAO format"),
                    @ApiResponse(responseCode = "404", description = "Airport not found"),
                    @ApiResponse(responseCode = "503", description = "Upstream API unavailable")
            }
    )
    public ResponseEntity<AirportDetailsResponse> getAirport(@PathVariable String icao) {
        log.debug("GET /api/v1/airports/{}", icao);
        AirportDetailsResponse response = airportService.getAirportDetails(icao);
        return ResponseEntity.ok(response);
    }
}
