package com.fabiankevin.app.config;

import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.*;
import com.fabiankevin.app.services.summaries.CategorySummaryGenerator;
import com.fabiankevin.app.services.summaries.DailySummaryGenerator;
import com.fabiankevin.app.services.summaries.MonthlySummaryGenerator;
import com.fabiankevin.app.services.summaries.YearlySummaryGenerator;
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
    public AccountService accountService(CacheManager cacheManager, DefaultAccountService delegate) {
        return new CachedAccountService(cacheManager, delegate);
    }

    @Bean
    public TransactionService transactionService(AccountRepository accountRepository,
                                                 CategoryRepository categoryRepository,
                                                 TransactionRepository transactionRepository,
                                                 CategorySummaryGenerator categorySummaryGenerator,
                                                 YearlySummaryGenerator yearlySummaryGenerator,
                                                 MonthlySummaryGenerator monthlySummaryGenerator,
                                                 DailySummaryGenerator dailySummaryGenerator) {
        return new DefaultTransactionService(accountRepository, categoryRepository, transactionRepository,
                List.of(categorySummaryGenerator,
                        yearlySummaryGenerator,
                        monthlySummaryGenerator,
                        dailySummaryGenerator));
    }
}
