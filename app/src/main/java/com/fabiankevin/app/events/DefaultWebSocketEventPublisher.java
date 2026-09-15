package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DefaultWebSocketEventPublisher implements EventPublisher {
    private final SimpMessagingTemplate template;
    private final String destination;

    @Override
    public void publish(UUID targetId, DomainEvent<?> event) {
        template.convertAndSendToUser(
                targetId.toString(),
                destination,
                event
        );
    }
}
