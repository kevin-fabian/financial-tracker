package com.fabiankevin.app.models.recurring_transactions;

import com.fabiankevin.app.accounts.domain.models.Account;
import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.models.User;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
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
        LocalDate nextOccurrenceDate,
        LocalDate endDate,
        int remainingDays,
        TransactionStatus transactionStatus,
        RecurringTransactionStatus status,
        User updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public RecurringTransaction toRecurringTransaction() {
        return new RecurringTransaction(
                id,
                null,
                description,
                amount,
                variableAmount,
                category,
                account,
                dayOfMonth,
                nextOccurrenceDate,
                endDate,
                status,
                createdAt,
                updatedAt
        );
    }
}
