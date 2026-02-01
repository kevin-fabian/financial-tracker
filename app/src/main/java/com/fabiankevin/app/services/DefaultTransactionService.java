package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class DefaultTransactionService implements TransactionService {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    @Override
    public Transaction addTransaction(AddTransactionCommand command) {
        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(AccountNotFoundException::new);
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(CategoryNotFoundException::new);

        Transaction transaction = Transaction.builder()
                .category(category)
                .account(account)
                .description(command.description())
                .amount(command.amount())
                .type(command.type())
                .transactionDate(command.transactionDate())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return transactionRepository.save(transaction);
    }
}
