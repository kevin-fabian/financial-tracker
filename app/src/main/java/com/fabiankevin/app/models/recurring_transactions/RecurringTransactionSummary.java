package com.fabiankevin.app.models.recurring_transactions;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransactionSummary(
        UUID id,
        String description,
        double amount,
        TransactionType transactionType,
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
