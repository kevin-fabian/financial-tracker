package com.fabiankevin.app.exceptions;

public class TransactionNotFoundException extends BusinessRuleException {
    public TransactionNotFoundException() {
        super("Transaction is not found");
    }
}

