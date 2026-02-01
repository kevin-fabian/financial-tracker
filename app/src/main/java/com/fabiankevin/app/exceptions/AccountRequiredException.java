package com.fabiankevin.app.exceptions;

public class AccountRequiredException extends BusinessRuleException {
    public AccountRequiredException() {
        super("Invalid account");
    }
}
