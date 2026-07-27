package com.fabiankevin.app.models.recurring_transactions;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import lombok.Builder;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransactionSummary(
        UUID id,
        UUID userId,
        String description,
        double amount,
        Category category,
        Account account,
        int dayOfMonth,
        ZonedDateTime nextOccurrenceDate,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        TransactionStatus transactionStatus,
        RecurringTransactionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
