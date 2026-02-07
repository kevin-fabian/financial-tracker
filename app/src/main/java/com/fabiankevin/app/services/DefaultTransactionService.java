package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.exceptions.TransactionNotFoundException;
import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DefaultTransactionService implements TransactionService {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final Map<SummaryType, SummaryGenerator> generators;

    public DefaultTransactionService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            List<SummaryGenerator> generators
    ) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.generators = generators.stream()
                .collect(Collectors.toMap(
                        SummaryGenerator::supports,
                        Function.identity()
                ));
    }

    @Override
    public void deleteTransaction(UUID transactionId, UUID userId) {
        int affectedRows = transactionRepository.deleteByIdAndUserId(transactionId, userId);
        if(affectedRows > 0){
            log.info("Deleted transaction with id {} for user {}", transactionId, userId);
        }
    }

    @Transactional
    @Override
    public Transaction addTransaction(AddTransactionCommand command) {
        UUID userId = command.userId();
        Account account = accountRepository.findById(command.accountId())
                .filter(acc -> acc.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);
        Category category = categoryRepository.findById(command.categoryId())
                .filter(cat -> cat.userId().equals(userId))
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

    @Transactional
    @Override
    public Transaction patchTransaction(PatchTransactionCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(TransactionNotFoundException::new);

        // validate and fetch new account if provided
        Account newAccount = null;
        if (command.accountId() != null) {
            newAccount = accountRepository.findById(command.accountId())
                    .filter(a -> a.userId().equals(userId))
                    .orElseThrow(AccountNotFoundException::new);
        }

        // validate and fetch new category if provided
        Category newCategory = null;
        if (command.categoryId() != null) {
            newCategory = categoryRepository.findById(command.categoryId())
                    .filter(c -> c.userId().equals(userId))
                    .orElseThrow(CategoryNotFoundException::new);
        }

        Transaction.TransactionBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(newAccount).ifPresent(builder::account);
        Optional.ofNullable(command.type()).ifPresent(builder::type);
        Optional.ofNullable(command.description()).ifPresent(builder::description);
        Optional.ofNullable(newCategory).ifPresent(builder::category);
        Optional.ofNullable(command.amount()).ifPresent(builder::amount);
        Optional.ofNullable(command.transactionDate()).ifPresent(builder::transactionDate);

        return transactionRepository.save(builder.build());
    }

    @Override
    public SummarySeries getSummary(SummaryQuery query) {
        SummaryGenerator generator = generators.get(query.type());
        if (generator == null) {
            throw new IllegalArgumentException("No generator found for type: " + query.type());
        }

        return new SummarySeries(
                query.type(),
                generator.generate(query)
        );
    }

    @Override
    public Page<Transaction> getTransactionsByPageQuery(PageQuery query, UUID userId) {
        return transactionRepository.getTransactionsByPageAndUserId(query, userId);
    }
}
