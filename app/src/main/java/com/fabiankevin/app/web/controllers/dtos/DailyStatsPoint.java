package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Daily statistics grouped by day of month")
public record DailyStatsPoint(
        @Schema(description = "Day of month (1-31)", example = "15")
        String label,

        @Schema(description = "Total expenses for this day of month", example = "250.00")
        double totalExpenses,

        @Schema(description = "Total income for this day of month", example = "1500.00")
        double totalIncome
) {
}
