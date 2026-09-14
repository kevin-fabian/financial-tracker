package com.fabiankevin.app.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CompositeHouseholdEventPublisher implements HouseholdEventPublisher {
    private final List<HouseholdEventPublisher> householdEventPublishers;

    public CompositeHouseholdEventPublisher(List<HouseholdEventPublisher> householdEventPublishers) {
        this.householdEventPublishers = householdEventPublishers;
    }

    @Async
    @Override
    public void publish(UUID householdId, DomainEvent<?> event) {
        for (HouseholdEventPublisher publisher : householdEventPublishers) {
            try {
                publisher.publish(householdId, event);
            } catch (Exception e) {
                log.warn("Event publisher failed for party {}: {}", householdId, e.getMessage());
            }
        }
    }
}
