package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import com.fabiankevin.app.services.recurring_transactions.commands.UpdateRecurringTransactionCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SyncChangeAwareRecurringTransactionService implements RecurringTransactionService {
    private final RecurringTransactionService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareRecurringTransactionService(
            @Qualifier("defaultRecurringTransactionService")
            RecurringTransactionService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public RecurringTransactionSummary create(final CreateRecurringTransactionCommand command) {
        final RecurringTransactionSummary result = delegate.create(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.RECURRING_TRANSACTIONS)
                .entityId(result.id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(jsonMapper.writeValueAsString(result.toRecurringTransaction()))
                .build());
        return result;
    }

    @Override
    @Transactional
    public RecurringTransactionSummary updateRecurringTransaction(final UpdateRecurringTransactionCommand command) {
        final RecurringTransactionSummary result = delegate.updateRecurringTransaction(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.RECURRING_TRANSACTIONS)
                .entityId(result.id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(jsonMapper.writeValueAsString(result.toRecurringTransaction()))
                .build());
        return result;
    }

    @Override
    public List<RecurringTransactionSummary> getRecurringTransactionsByUserId(final UUID userId) {
        return delegate.getRecurringTransactionsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteRecurringTransactionById(final UUID id, final UUID userId) {
        final List<RecurringTransactionSummary> all = delegate.getRecurringTransactionsByUserId(userId);
        final Optional<RecurringTransactionSummary> existing = all.stream()
                .filter(rt -> rt.id().equals(id))
                .findFirst();
        delegate.deleteRecurringTransactionById(id, userId);
        existing.ifPresent(rt -> syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.RECURRING_TRANSACTIONS)
                .entityId(id)
                .operation(SyncChangeOperationType.DELETE)
                .payload(jsonMapper.writeValueAsString(rt.toRecurringTransaction()))
                .build()));
    }

    @Override
    public void deleteAllByUserId(final UUID userId) {
        delegate.deleteAllByUserId(userId);
    }

    @Override
    public void processDueRecurringTransactions() {
        delegate.processDueRecurringTransactions();
    }
}
