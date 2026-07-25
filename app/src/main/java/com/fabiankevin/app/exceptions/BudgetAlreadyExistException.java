package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class BudgetAlreadyExistException extends ApiException {
    public BudgetAlreadyExistException(String message) {
        super(message, 409);
    }
}
