package com.fabiankevin.app.services;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import com.fabiankevin.app.services.recurring_transactions.commands.UpdateRecurringTransactionCommand;

import java.util.List;
import java.util.UUID;

public interface RecurringTransactionService {
    RecurringTransactionSummary create(CreateRecurringTransactionCommand command);

    List<RecurringTransactionSummary> getRecurringTransactionsByUserId(UUID userId);

    void deleteRecurringTransactionById(UUID id, UUID userId);

    RecurringTransactionSummary updateRecurringTransaction(UpdateRecurringTransactionCommand command);

    void processDueRecurringTransactions();
}
