package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for financial statistics")
public record StatsResponse(
        @Schema(description = "Total account balance (income minus expenses)", example = "15000.50")
        double totalBalance,

        @Schema(description = "Total expenses in the queried period", example = "5000.00")
        double totalExpenses,

        @Schema(description = "Total income in the queried period", example = "20000.50")
        double totalIncome,

        @Schema(description = "Growth percentage compared to prior period (0.0 if prior period is zero)", example = "12.5")
        double growthPercentage
) {
}
