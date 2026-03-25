package com.aviation.airport.mapper;

import com.aviation.airport.dto.AirportDetailsResponse;
import com.aviation.airport.dto.AirportDetailsResponse.ChartReference;
import com.aviation.airport.dto.AirportDetailsResponse.Charts;
import com.aviation.airport.feign.dto.AviationApiResponse;
import com.aviation.airport.feign.dto.AviationApiResponse.Chart;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Maps the raw upstream {@link AviationApiResponse} to the public
 * {@link AirportDetailsResponse} contract.
 *
 * <p>Keeping mapping logic here (rather than in the service) makes it easy to
 * unit-test the transformation independently of HTTP or resilience concerns.
 */
@Component
public class AirportMapper {

    public AirportDetailsResponse toResponse(AviationApiResponse raw) {
        var data = raw.airportData();

        return new AirportDetailsResponse(
                data.icaoIdent(),
                data.faaIdent(),
                toTitleCase(data.airportName()),
                toTitleCase(data.city()),
                data.stateAbbr(),
                data.country(),
                data.isMilitary(),
                toCharts(raw.charts())
        );
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Charts toCharts(AviationApiResponse.Charts raw) {
        if (raw == null) {
            return new Charts(null, null, null, null, null);
        }
        return new Charts(
                mapCharts(raw.airportDiagram()),
                mapCharts(raw.general()),
                mapCharts(raw.departure()),
                mapCharts(raw.arrival()),
                mapCharts(raw.approach())
        );
    }

    private List<ChartReference> mapCharts(List<Chart> charts) {
        return Optional.ofNullable(charts)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toChartReference)
                .toList();
    }

    private ChartReference toChartReference(Chart c) {
        return new ChartReference(
                c.chartName(),
                c.pdfUrl(),
                c.didChange(),
                c.didChange() ? c.changePdfUrl() : null   // omit field when unchanged
        );
    }

    /**
     * Converts "JOHN F KENNEDY INTL" → "John F Kennedy Intl" for readability.
     * Falls back to the original string on null input.
     */
    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        return java.util.Arrays.stream(input.split("\\s+"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
