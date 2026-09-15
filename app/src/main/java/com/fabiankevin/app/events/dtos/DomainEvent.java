package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;

public interface DomainEvent<T> {
    EventAction action();
    T payload();
}
