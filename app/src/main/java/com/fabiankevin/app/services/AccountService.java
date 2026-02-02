package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.services.commands.CreateAccountCommand;

import java.util.UUID;

public interface AccountService {
    Account getAccountById(UUID id, UUID userId);
    Account createAccount(CreateAccountCommand command);
    void deleteAccountById(UUID id, UUID userId);
}
