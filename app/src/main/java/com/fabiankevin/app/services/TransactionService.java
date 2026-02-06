package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.queries.SummaryQuery;

public interface TransactionService {
    Transaction addTransaction(AddTransactionCommand command);
    SummarySeries getSummary(SummaryQuery query);
}
