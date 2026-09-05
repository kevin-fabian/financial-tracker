package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.enums.SummaryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing a summary series with data points")
public record SummarySeriesResponse(
        @Schema(description = "Summary type", example = "DAILY")
        SummaryType type,
        @Schema(description = "List of summary data points",
                exampleClasses = SummaryPointResponse.class)
        List<SummaryPointResponse> points
) {
    public static SummarySeriesResponse from(SummarySeries summarySeries) {
        return new SummarySeriesResponse(summarySeries.type(), summarySeries.points().stream()
                .map(SummaryPointResponse::from)
                .toList());
    }
}
