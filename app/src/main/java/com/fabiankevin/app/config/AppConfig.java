package com.fabiankevin.app.config;

import com.fabiankevin.app.events.CompositeTransactionEventPublisher;
import com.fabiankevin.app.events.StatsEventPublisher;
import com.fabiankevin.app.events.TransactionEventPublisher;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.DefaultStatsService;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.PartyService;
import com.fabiankevin.app.services.StatsService;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {
//
//    @Bean
//    public CategoryService categoryService(CacheManager cacheManager, DefaultCategoryService delegate) {
//        return new CachedCategoryService(cacheManager, delegate);
//    }
//
//    @Bean
//    public TransactionService transactionService(CacheManager cacheManager,
//                                                 DefaultTransactionService delegate) {
//        return new CachedTransactionService(cacheManager, delegate);
//    }

    @Bean
    public StatsService statsService(TransactionRepository transactionRepository,
                                     PartyService partyService) {
        return new DefaultStatsService(transactionRepository, partyService);
    }

    @Bean
    public CompositeTransactionEventPublisher compositeEventPublisher(
            StatsEventPublisher statsEventPublisher,
            TransactionEventPublisher transactionEventPublisher) {
        return new CompositeTransactionEventPublisher(
                List.of(statsEventPublisher,
                        transactionEventPublisher));
    }

    @Bean
    public DefaultTransactionService defaultTransactionService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            List<SummaryGenerator> generators,
            PartyRepository partyRepository,
            CompositeTransactionEventPublisher compositeTransactionEventPublisher,
            @Value("${transaction.daily-limit:100}") int dailyTransactionLimit) {
        return new DefaultTransactionService(
                accountRepository,
                categoryRepository,
                transactionRepository,
                generators,
                partyRepository,
                compositeTransactionEventPublisher,
                dailyTransactionLimit);
    }
}
