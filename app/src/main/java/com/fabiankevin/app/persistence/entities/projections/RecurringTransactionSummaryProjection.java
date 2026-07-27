package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransactionSummaryProjection(
        UUID id,
        UUID userId,
        String description,
        double amount,
        int dayOfMonth,
        ZonedDateTime nextOccurrenceDate,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        String transactionStatus,
        String status,
        Instant createdAt,
        Instant updatedAt,
        UUID categoryId,
        String categoryName,
        String categoryType,
        UUID categoryUserId,
        String categoryIcon,
        boolean categoryActive,
        Instant categoryCreatedAt,
        Instant categoryUpdatedAt,
        UUID accountId,
        String accountName,
        UUID accountUserId,
        String accountCurrency,
        String accountType,
        boolean accountActive,
        Instant accountCreatedAt,
        Instant accountUpdatedAt
) {
}
