package com.fabiankevin.app.services;

import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;

import java.time.LocalDate;
import java.util.UUID;

public interface AccountService {
    AccountSummary getAccountById(UUID id, UUID userId);
    AccountSummary createAccount(CreateAccountCommand command);
    AccountSummary patchAccount(PatchAccountCommand command);
    void deleteAccountById(UUID id, UUID userId);
    void disableAccount(UUID id, UUID userId);
    Page<AccountSummary> getAccountSummariesByPageQuery(PageQuery query, UUID userId, LocalDate monthStart, LocalDate monthEnd);
    void deleteAllByUserId(UUID userId);
}
