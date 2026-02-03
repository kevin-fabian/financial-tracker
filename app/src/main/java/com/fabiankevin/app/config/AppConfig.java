package com.fabiankevin.app.config;

import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public TransactionService transactionService(AccountRepository accountRepository,
                                                 CategoryRepository categoryRepository,
                                                 TransactionRepository transactionRepository) {
        return new DefaultTransactionService(accountRepository, categoryRepository, transactionRepository,
                List.of());
    }
}
