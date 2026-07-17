package com.fabiankevin.app.config;

import com.fabiankevin.app.events.CompositeTransactionEventPublisher;
import com.fabiankevin.app.events.StatsEventPublisher;
import com.fabiankevin.app.events.TransactionEventPublisher;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.*;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public CategoryService categoryService(CacheManager cacheManager, DefaultCategoryService delegate) {
        return new CachedCategoryService(cacheManager, delegate);
    }

    @Bean
    public TransactionService transactionService(CacheManager cacheManager,
                                                 DefaultTransactionService delegate) {
        return new CachedTransactionService(cacheManager, delegate);
    }

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
            SharedSpaceRepository sharedSpaceRepository,
            CompositeTransactionEventPublisher compositeTransactionEventPublisher) {
        return new DefaultTransactionService(
                accountRepository,
                categoryRepository,
                transactionRepository,
                generators,
                sharedSpaceRepository,
                compositeTransactionEventPublisher);
    }
}
