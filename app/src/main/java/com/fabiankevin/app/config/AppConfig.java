package com.fabiankevin.app.config;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.DefaultHouseholdEventPublisher;
import com.fabiankevin.app.events.DefautlUserEventPublisher;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class AppConfig {

    private final SimpMessagingTemplate template;

    @Bean
    public StatsService statsService(TransactionRepository transactionRepository,
                                     HouseholdService householdService) {
        return new DefaultStatsService(transactionRepository, householdService);
    }

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
