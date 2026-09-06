package com.fabiankevin.app.events;

import com.fabiankevin.app.models.enums.EventAction;

import java.util.UUID;

public interface DomainEvent<T> {
    UUID userId();
    EventAction action();
    T payload();
}
