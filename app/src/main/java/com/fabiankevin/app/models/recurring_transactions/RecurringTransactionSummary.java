package com.fabiankevin.app.models.recurring_transactions;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import lombok.Builder;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransactionSummary(
        UUID id,
        String description,
        double amount,
        boolean variableAmount,
        Category category,
        Account account,
        int dayOfMonth,
        ZonedDateTime nextOccurrenceDate,
        ZonedDateTime endDate,
        int remainingDays,
        TransactionStatus transactionStatus,
        RecurringTransactionStatus status,
        User user,
        Instant createdAt,
        Instant updatedAt
) {
}
