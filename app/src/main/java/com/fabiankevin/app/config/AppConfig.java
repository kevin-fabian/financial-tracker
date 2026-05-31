package com.fabiankevin.app.config;

import com.fabiankevin.app.services.*;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public CategoryService categoryService(CacheManager cacheManager, DefaultCategoryService delegate) {
        return new CachedCategoryService(cacheManager, delegate);
    }

    @Bean
    public AccountService accountService(CacheManager cacheManager, DefaultAccountService delegate) {
        return new CachedAccountService(cacheManager, delegate);
    }

    @Bean
    public TransactionService transactionService(CacheManager cacheManager,
                                                 DefaultTransactionService delegate) {
        return new CachedTransactionService(cacheManager, delegate);
    }
}
