package com.fabiankevin.app.events;

import com.fabiankevin.app.events.dtos.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class TransactionEventPublisher implements EventPublisher {
    private final SimpMessagingTemplate template;

    @Override
    public void publish(UUID targetId, DomainEvent<?> event) {
        template.convertAndSend(String.format("/topic/households/%s/transactions", targetId), event);
    }
}
