package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.EventAction;

public record TransactionEventPayload(
    EventAction action,
    String userId,
    Transaction data
) implements EventPayload<Transaction> {
    @Override
    public Transaction payload() {
        return data;
    }
}
