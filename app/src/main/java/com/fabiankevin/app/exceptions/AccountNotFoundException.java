package com.fabiankevin.app.exceptions;

public class AccountNotFoundException extends NotFoundException {
    public AccountNotFoundException() {
        super("Account not found");
    }
}
