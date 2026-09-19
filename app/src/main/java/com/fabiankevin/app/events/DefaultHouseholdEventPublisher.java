package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.EventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.UUID;

@RequiredArgsConstructor
public class DefaultHouseholdEventPublisher implements EventPublisher {
    private final SimpMessagingTemplate template;
    private final String destination;

    @Async
    @Override
    public void publish(UUID targetId, EventPayload<?> event) {
        template.convertAndSend(String.format(destination, targetId), event);
    }
}
