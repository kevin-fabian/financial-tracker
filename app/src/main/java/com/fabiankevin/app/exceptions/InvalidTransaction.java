package com.fabiankevin.app.exceptions;

public class InvalidTransaction extends DomainException {
    public InvalidTransaction() {
        super("Invalid transaction");
    }
}
