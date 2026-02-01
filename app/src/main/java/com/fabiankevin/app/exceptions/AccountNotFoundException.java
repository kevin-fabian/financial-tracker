package com.fabiankevin.app.exceptions;

public class AccountNotFoundException extends BusinessRuleException {
    public AccountNotFoundException() {
        super("Account not found");
    }
}
