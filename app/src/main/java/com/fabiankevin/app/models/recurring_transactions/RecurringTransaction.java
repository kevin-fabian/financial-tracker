package com.fabiankevin.app.models.recurring_transactions;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record RecurringTransaction(
        UUID id,
        UUID userId,
        String description,
        double amount,
        boolean variableAmount,
        Category category,
        Account account,
        int dayOfMonth,
        LocalDate nextOccurrenceDate,
        LocalDate endDate,
        RecurringTransactionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
