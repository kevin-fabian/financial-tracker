package com.fabiankevin.app.config;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.DefaultWebSocketEventPublisher;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.HouseholdRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.DefaultStatsService;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.StatsService;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public StatsService statsService(TransactionRepository transactionRepository,
                                     HouseholdService householdService) {
        return new DefaultStatsService(transactionRepository, householdService);
    }

    @Bean
    public DefaultWebSocketEventPublisher invitationEventPublisher(SimpMessagingTemplate template) {
        return new DefaultWebSocketEventPublisher(template, "/queue/household-invitations");
    }

    @Bean
    public DefaultTransactionService defaultTransactionService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            List<SummaryGenerator> generators,
            HouseholdRepository householdRepository,
            EventPublisher transactionEventPublisher,
            @Value("${transaction.daily-limit:100}") int dailyTransactionLimit,
            UserClient userClient) {
        return new DefaultTransactionService(
                accountRepository,
                categoryRepository,
                transactionRepository,
                generators,
                householdRepository,
                transactionEventPublisher,
                dailyTransactionLimit,
                userClient);
    }
}
