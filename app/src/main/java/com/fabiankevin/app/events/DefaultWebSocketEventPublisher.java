package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DefaultWebSocketEventPublisher implements EventPublisher {
    private final SimpMessagingTemplate template;
    private final String destination;

    @Async
    @Override
    public void publish(UUID targetId, EventPayload<?> event) {
        template.send(destination, MessageBuilder
                .withPayload(event)
                .build());
    }
}
