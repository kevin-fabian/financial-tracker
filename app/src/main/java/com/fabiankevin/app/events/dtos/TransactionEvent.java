package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.EventAction;

public record TransactionEvent(
    EventAction action,
    Transaction data
) implements DomainEvent<Transaction> {
    @Override
    public Transaction payload() {
        return data;
    }
}
