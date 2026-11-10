package com.fabiankevin.app.accounts.domain.services;

import com.fabiankevin.app.accounts.domain.models.AccountSummary;
import com.fabiankevin.app.accounts.domain.services.commands.CreateAccountCommand;
import com.fabiankevin.app.accounts.domain.services.commands.PatchAccountCommand;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeAwareAccountService implements AccountService {
    private final AccountService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareAccountService(
            @Qualifier("defaultAccountService")
            AccountService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public AccountSummary getAccountById(final UUID id, final UUID userId) {
        return delegate.getAccountById(id, userId);
    }

    @Override
    public AccountSummary createAccount(final CreateAccountCommand command) {
        final AccountSummary result = delegate.createAccount(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.ACCOUNTS)
                .entityId(result.account().id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(jsonMapper.writeValueAsString(result.account()))
                .build());
        return result;
    }

    @Override
    public void createBatchAccounts(final List<CreateAccountCommand> commands, final UUID userId) {
        delegate.createBatchAccounts(commands, userId);
    }

    @Override
    public AccountSummary patchAccount(final PatchAccountCommand command) {
        final AccountSummary result = delegate.patchAccount(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.ACCOUNTS)
                .entityId(result.account().id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(jsonMapper.writeValueAsString(result.account()))
                .build());
        return result;
    }

    @Override
    public void deleteAccountById(final UUID id, final UUID userId) {
        delegate.deleteAccountById(id, userId);
    }

    @Override
    public void disableAccount(final UUID id, final UUID userId) {
        final AccountSummary existing = delegate.getAccountById(id, userId);
        delegate.disableAccount(id, userId);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.ACCOUNTS)
                .entityId(id)
                .operation(SyncChangeOperationType.DELETE)
                .payload(jsonMapper.writeValueAsString(existing))
                .build());
    }

    @Override
    public Page<AccountSummary> getAccountSummariesByPageQuery(final PageQuery query, final UUID userId,
            final LocalDate monthStart, final LocalDate monthEnd) {
        return delegate.getAccountSummariesByPageQuery(query, userId, monthStart, monthEnd);
    }

    @Override
    public void deleteAllByUserId(final UUID userId) {
        delegate.deleteAllByUserId(userId);
    }
}
