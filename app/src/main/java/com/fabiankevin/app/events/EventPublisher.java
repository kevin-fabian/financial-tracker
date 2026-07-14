package com.fabiankevin.app.events;

import com.fabiankevin.app.models.ItemEvent;

import java.util.UUID;

public interface EventPublisher<T> {
    void publish(UUID sharedId, ItemEvent<T> event);
}
