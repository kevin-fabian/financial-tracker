package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;

public interface RecurringTransactionRepository {
    RecurringTransaction save(RecurringTransaction recurringTransaction);
}
