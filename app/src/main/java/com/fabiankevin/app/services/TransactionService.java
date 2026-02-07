package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;

import java.util.UUID;

public interface TransactionService {
    Transaction addTransaction(AddTransactionCommand command);
    SummarySeries getSummary(SummaryQuery query);

    Page<Transaction> getTransactionsByPageQuery(PageQuery query, UUID userId);
    Transaction patchTransaction(PatchTransactionCommand command);
    void deleteTransaction(UUID transactionId, UUID userId);
}
