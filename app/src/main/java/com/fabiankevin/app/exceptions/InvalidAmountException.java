package com.fabiankevin.app.exceptions;

public class InvalidAmountException extends DomainException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
