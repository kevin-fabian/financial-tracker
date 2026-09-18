package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.EventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class TransactionEventPublisher implements EventPublisher {
    private final SimpMessagingTemplate template;

    @Override
    public void publish(UUID targetId, EventPayload<?> event) {
        template.convertAndSend(String.format("/ws/households/%s/transactions", targetId), event);
    }
}
