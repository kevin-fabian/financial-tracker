package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CompositeEventPublisher implements EventPublisher {
    private final List<EventPublisher> eventPublishers;

    public CompositeEventPublisher(List<EventPublisher> eventPublishers) {
        this.eventPublishers = eventPublishers;
    }

    @Async
    @Override
    public void publish(UUID targetId, DomainEvent<?> event) {
        for (EventPublisher publisher : eventPublishers) {
            try {
                publisher.publish(targetId, event);
            } catch (Exception e) {
                log.warn("Event publisher failed for party {}: {}", targetId, e.getMessage());
            }
        }
    }
}
