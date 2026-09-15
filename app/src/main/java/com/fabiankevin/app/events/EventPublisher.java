package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.DomainEvent;

import java.util.UUID;

public interface EventPublisher {
    void publish(UUID targetId, DomainEvent<?> event);
}
