package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;

public interface EventPayload<T> {
    EventAction action();
    String userId();
    T payload();
}
