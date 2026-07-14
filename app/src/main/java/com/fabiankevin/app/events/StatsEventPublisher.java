package com.fabiankevin.app.events;

import com.fabiankevin.app.models.ItemEvent;
import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.services.StatsService;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class StatsEventPublisher implements EventPublisher<Transaction> {
    private final SimpMessagingTemplate template;
    private final JsonMapper jsonMapper;
    private final StatsService statsService;

    @Override
    public void publish(UUID sharedId, ItemEvent<Transaction> event) {
        StatsSummary statsSummary = statsService.getStatsSummary(event.usedId(), StatsQuery.builder().build());
        template.convertAndSend(String.format("/topic/spaces/%s/stats",
                        sharedId),
                jsonMapper.writeValueAsString(new ItemEvent<>(
                        event.usedId(),
                        event.action(),
                        statsSummary
                )));
    }
}
