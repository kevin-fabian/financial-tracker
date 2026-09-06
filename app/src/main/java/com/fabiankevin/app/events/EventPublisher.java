package com.fabiankevin.app.events;

import java.util.UUID;

public interface EventPublisher {
    void publish(UUID partyId, DomainEvent<?> event);
}
