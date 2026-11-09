package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.web.controllers.dtos.transactions.TransactionResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeAwareTransactionService implements TransactionService {
    private final TransactionService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareTransactionService(
            @Qualifier("defaultTransactionService")
            TransactionService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void addTransactions(final List<AddTransactionCommand> commands) {
        delegate.addTransactions(commands);
    }

    @Override
    public int deleteAllByUserId(final UUID userId) {
        return delegate.deleteAllByUserId(userId);
    }

    @Override
    public Transaction addTransaction(final AddTransactionCommand command) {
        final Transaction result = delegate.addTransaction(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.TRANSACTIONS)
                .entityId(result.id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(jsonMapper.writeValueAsString(result))
                .build());
        return result;
    }

    @Override
    public SummarySeries getSummary(final SummaryQuery query) {
        return delegate.getSummary(query);
    }

    @Override
    public Page<Transaction> getTransactionsByPageQuery(final PageQuery query, final UUID userId,
            final TransactionType type) {
        return delegate.getTransactionsByPageQuery(query, userId, type);
    }

    @Override
    public Transaction patchTransaction(final PatchTransactionCommand command) {
        final Transaction result = delegate.patchTransaction(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.TRANSACTIONS)
                .entityId(result.id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(jsonMapper.writeValueAsString(result))
                .build());
        return result;
    }

    @Override
    public void deleteTransaction(final UUID transactionId, final UUID userId) {
        delegate.deleteTransaction(transactionId, userId);
    }

    @Override
    public TransactionResponse getTransactionById(final UUID id, final UUID userId) {
        return delegate.getTransactionById(id, userId);
    }
}
