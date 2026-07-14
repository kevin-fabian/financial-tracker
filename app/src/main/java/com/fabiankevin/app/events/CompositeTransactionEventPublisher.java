package com.fabiankevin.app.events;

import com.fabiankevin.app.models.ItemEvent;
import com.fabiankevin.app.models.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CompositeTransactionEventPublisher implements EventPublisher<Transaction> {
    private final List<EventPublisher<Transaction>> eventPublishers;

    @Async
    @Override
    public void publish(UUID sharedId, ItemEvent<Transaction> event) {
        for (EventPublisher<Transaction> eventPublisher : eventPublishers) {
            eventPublisher.publish(sharedId, event);
        }
    }
}
