package com.fabiankevin.app.accounts.domain.exceptions;

import com.fabiankevin.app.exceptions.NotFoundException;

public class AccountNotFoundException extends NotFoundException {
    public AccountNotFoundException() {
        super("Account not found");
    }
}
