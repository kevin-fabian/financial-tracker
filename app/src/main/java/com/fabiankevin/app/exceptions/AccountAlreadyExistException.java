package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class AccountAlreadyExistException extends ApiException {
    public AccountAlreadyExistException(String message) {
        super(message, 409);
    }
}
