package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.UUID;

public interface AccountService {
    Account getAccountById(UUID id, UUID userId);
    Account createAccount(CreateAccountCommand command);
    void deleteAccountById(UUID id, UUID userId);
    Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId);
}
