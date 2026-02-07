package com.fabiankevin.app.config;

import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.summaries.CategorySummaryGenerator;
import com.fabiankevin.app.services.summaries.DailySummaryGenerator;
import com.fabiankevin.app.services.summaries.MonthlySummaryGenerator;
import com.fabiankevin.app.services.summaries.YearlySummaryGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

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
