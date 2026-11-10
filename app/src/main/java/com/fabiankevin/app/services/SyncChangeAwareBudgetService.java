package com.fabiankevin.app.services;

import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeAwareBudgetService implements BudgetService {
    private final BudgetService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareBudgetService(
            @Qualifier("defaultBudgetService")
            BudgetService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public BudgetSummary getBudgetSummaryByIdAndUserId(final UUID id, final UUID userId) {
        return delegate.getBudgetSummaryByIdAndUserId(id, userId);
    }

    @Override
    @Transactional
    public BudgetSummary createBudget(final CreateBudgetCommand command) {
        final BudgetSummary result = delegate.createBudget(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.BUDGETS)
                .entityId(result.budget().id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(jsonMapper.writeValueAsString(result.toBudget()))
                .build());
        return result;
    }

    @Override
    public List<BudgetSummary> getBudgetsByUserId(final UUID userId) {
        return delegate.getBudgetsByUserId(userId);
    }

    @Override
    @Transactional
    public BudgetSummary patchBudget(final PatchBudgetCommand command) {
        final BudgetSummary result = delegate.patchBudget(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.BUDGETS)
                .entityId(result.budget().id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(jsonMapper.writeValueAsString(result.toBudget()))
                .build());
        return result;
    }

    @Override
    @Transactional
    public void deleteBudgetById(final UUID id, final UUID userId) {
        final BudgetSummary existing = delegate.getBudgetSummaryByIdAndUserId(id, userId);
        delegate.deleteBudgetById(id, userId);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.BUDGETS)
                .entityId(id)
                .operation(SyncChangeOperationType.DELETE)
                .payload(jsonMapper.writeValueAsString(existing.toBudget()))
                .build());
    }

    @Override
    public void deleteAllByUserId(final UUID userId) {
        delegate.deleteAllByUserId(userId);
    }
}
