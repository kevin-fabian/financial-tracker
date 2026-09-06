package com.fabiankevin.app.events;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.EventAction;

import java.util.UUID;

public record TransactionEvent(
    UUID userId,
    EventAction action,
    Transaction data
) implements DomainEvent<Transaction> {
    @Override
    public Transaction payload() {
        return data;
    }
}
