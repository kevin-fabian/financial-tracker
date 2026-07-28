package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface RecurringTransactionRepository {
    RecurringTransaction save(RecurringTransaction recurringTransaction);

    List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, ZonedDateTime now);

    Stream<RecurringTransaction> streamDueRecurringTransactions(ZonedDateTime now);

    int deleteByIdAndUserId(UUID id, UUID userId);

    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);
}
