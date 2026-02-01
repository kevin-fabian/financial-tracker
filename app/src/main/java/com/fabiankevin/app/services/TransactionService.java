package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.services.commands.AddTransactionCommand;

public interface TransactionService {
    Transaction addTransaction(AddTransactionCommand command);
}
