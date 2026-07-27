package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface RecurringTransactionRepository {
    RecurringTransaction save(RecurringTransaction recurringTransaction);

    List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, ZonedDateTime now);
}
