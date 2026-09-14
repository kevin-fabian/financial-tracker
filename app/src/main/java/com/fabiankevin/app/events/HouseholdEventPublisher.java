package com.fabiankevin.app.events;

import java.util.UUID;

public interface HouseholdEventPublisher {
    void publish(UUID householdId, DomainEvent<?> event);
}
