package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.EventPayload;

import java.util.UUID;

public interface EventPublisher {
    void publish(UUID targetId, EventPayload<?> event);
}
