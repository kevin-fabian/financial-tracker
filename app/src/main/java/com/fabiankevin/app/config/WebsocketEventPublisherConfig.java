package com.fabiankevin.app.config;

import com.fabiankevin.app.events.DefaultHouseholdEventPublisher;
import com.fabiankevin.app.events.DefautlUserEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@RequiredArgsConstructor
@Configuration
public class WebsocketEventPublisherConfig {
    private final SimpMessagingTemplate template;

    @Bean
    public DefaultHouseholdEventPublisher householdEventPublisher() {
        return new DefaultHouseholdEventPublisher(template, "/topic/households/%s");
    }

    @Bean
    public DefaultHouseholdEventPublisher transactionEventPublisher() {
        return new DefaultHouseholdEventPublisher(template, "/topic/households/%s/transactions");
    }

    @Bean
    public DefautlUserEventPublisher invitationEventPublisher() {
        return new DefautlUserEventPublisher(template, "/queue/household-invitations");
    }

    @Bean
    public DefautlUserEventPublisher shoppingListEventPublisher() {
        return new DefautlUserEventPublisher(template, "/queue/shopping-lists");
    }
}
