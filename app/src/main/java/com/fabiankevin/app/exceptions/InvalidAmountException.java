package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class InvalidAmountException extends DomainException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
