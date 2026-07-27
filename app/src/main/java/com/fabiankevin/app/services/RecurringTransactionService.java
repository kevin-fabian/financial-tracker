package com.fabiankevin.app.services;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;

public interface RecurringTransactionService {
    RecurringTransactionSummary create(CreateRecurringTransactionCommand command);
}
