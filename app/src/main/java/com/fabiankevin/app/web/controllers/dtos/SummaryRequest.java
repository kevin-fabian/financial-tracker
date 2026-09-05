package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.queries.SummaryQuery;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Request DTO for generating transaction summaries")
public record SummaryRequest(
        @Schema(description = "Summary type", example = "DAILY")
        SummaryType type,
        @Schema(description = "Start date for summary", example = "2025-01-01")
        LocalDate from,
        @Schema(description = "End date for summary", example = "2025-01-31")
        LocalDate to,
        @Schema(description = "Transaction type filter", example = "EXPENSE")
        TransactionType transactionType) {

    public SummaryQuery toCommand(Set<UUID> userIds) {
        return new SummaryQuery(type, from, to, userIds, transactionType);
    }
}
