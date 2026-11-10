package com.fabiankevin.app.accounts.domain.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class AccountAlreadyExistException extends ApiException {
    public AccountAlreadyExistException(String message) {
        super(message, 409);
    }
}
