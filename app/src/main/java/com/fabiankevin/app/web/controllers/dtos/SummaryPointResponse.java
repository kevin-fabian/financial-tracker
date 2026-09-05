package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SummaryPoint;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO representing a single data point in a summary series")
public record SummaryPointResponse(
        @Schema(description = "Label for the data point", example = "2025-01-15")
        String label,
        @Schema(description = "Total value for this data point", example = "1500.50")
        double total) {
    public static SummaryPointResponse from(SummaryPoint point) {
        return new SummaryPointResponse(point.label(), point.total());
    }
}
