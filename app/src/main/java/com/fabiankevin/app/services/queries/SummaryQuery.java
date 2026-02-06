package com.fabiankevin.app.services.queries;

import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record SummaryQuery(
    SummaryType type,
    LocalDate from,
    LocalDate to,
    List<UUID> userIds,
    TransactionType transactionType
) {
}
