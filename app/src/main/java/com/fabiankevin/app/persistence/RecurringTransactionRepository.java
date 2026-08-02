package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface RecurringTransactionRepository {
    RecurringTransaction save(RecurringTransaction recurringTransaction);

    List<RecurringTransaction> saveAll(List<RecurringTransaction> recurringTransactions);

    List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, LocalDate now);

    Stream<RecurringTransaction> streamDueRecurringTransactions(LocalDate now);

    int deleteByIdAndUserId(UUID id, UUID userId);

    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);
}
