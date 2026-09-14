package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response DTO for daily statistics grouped by day of month")
public record DailyStatsResponse(
        @Schema(description = "List of daily statistics grouped by day of month", example = """
                [
                  {"label": "1", "totalExpenses": 0.0, "totalIncome": 100.0},
                  {"label": "15", "totalExpenses": 50.0, "totalIncome": 0.0}
                ]
                """)
        List<DailyStatsPoint> data
) {
}
