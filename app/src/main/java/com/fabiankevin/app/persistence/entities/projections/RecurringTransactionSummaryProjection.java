package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransactionSummaryProjection(
        UUID id,
        String description,
        double amount,
        int dayOfMonth,
        LocalDate nextOccurrenceDate,
        LocalDate endDate,
        String transactionStatus,
        String status,
        UUID updatedBy,
        Instant createdAt,
        Instant updatedAt,
        CategoryEntity category,
        AccountEntity account
) {
}
