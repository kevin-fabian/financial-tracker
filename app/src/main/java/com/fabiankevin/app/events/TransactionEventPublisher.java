package com.fabiankevin.app.events;

import com.fabiankevin.app.models.ItemEvent;
import com.fabiankevin.app.models.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class TransactionEventPublisher implements EventPublisher<Transaction> {
    private final SimpMessagingTemplate template;
    private final JsonMapper jsonMapper;

    @Override
    public void publish(UUID sharedId, ItemEvent<Transaction> event) {
        template.convertAndSend(String.format("/topic/spaces/%s/transactions",
                        sharedId),
                jsonMapper.writeValueAsString(event));

    }
}
