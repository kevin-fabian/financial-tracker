package com.fabiankevin.app.exceptions;

public class TransactionNotFoundException extends NotFoundException {
    public TransactionNotFoundException() {
        super("Transaction is not found");
    }
}

