package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Query DTO for filtering stats queries")
public record StatsQuery(
        @Schema(description = "Start date for filtering transactions (inclusive)", example = "2025-01-01")
        LocalDate fromDate,

        @Schema(description = "End date for filtering transactions (inclusive)", example = "2025-01-31")
        LocalDate toDate,

        @Schema(description = "Account id to filter by", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,

        @Schema(description = "Category id to filter by", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId
) {
}
