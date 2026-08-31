package com.fabiankevin.app.services;

import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.exceptions.*;
import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import com.fabiankevin.app.web.controllers.dtos.TransactionResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DefaultTransactionService implements TransactionService {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final Map<SummaryType, SummaryGenerator> generators;
    private final PartyRepository partyRepository;
    private final EventPublisher<Transaction> compositeEventPublisher;
    private final int dailyTransactionLimit;

    public DefaultTransactionService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            List<SummaryGenerator> generators,
            PartyRepository partyRepository,
            EventPublisher<Transaction> compositeEventPublisher,
            int dailyTransactionLimit) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.generators = generators.stream()
                .collect(Collectors.toMap(
                        SummaryGenerator::supports,
                        Function.identity()
                ));
        this.partyRepository = partyRepository;
        this.compositeEventPublisher = compositeEventPublisher;
        this.dailyTransactionLimit = dailyTransactionLimit;
    }

    @Transactional
    @Override
    public void deleteTransaction(UUID transactionId, UUID userId) {
        transactionRepository.deleteByIdAndUserId(transactionId, userId);
    }

    @Override
    public TransactionResponse getTransactionById(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.account().userId().equals(userId))
                .orElseThrow(TransactionNotFoundException::new);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    @Override
    public Transaction addTransaction(AddTransactionCommand command) {
        UUID userId = command.userId();
        LocalDate today = LocalDate.now();
        long existingCount = transactionRepository.countByUserIdAndCreatedAtOnDate(userId, today);
        if (existingCount >= dailyTransactionLimit) {
            throw new DailyTransactionLimitExceededException(dailyTransactionLimit);
        }
        if (command.amount() <= 0) {
            throw new InvalidAmountException("amount must be greater than zero");
        }
        Account account = accountRepository.findById(command.accountId())
                .filter(acc -> acc.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);
        Category category = categoryRepository.findById(command.categoryId())
                .filter(cat -> userId.equals(cat.userId()) || cat.system())
                .orElseThrow(CategoryNotFoundException::new);

        Transaction transaction = Transaction.builder()
                .category(category)
                .account(account)
                .description(command.description())
                .amount(command.amount())
                .type(category.type())
                .transactionDate(command.transactionDate())
                .recurringTransactionId(command.recurringTransactionId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        partyRepository.findByPlayerId(userId).ifPresent(party -> {
            compositeEventPublisher.publish(party.id(), new ItemEvent<>(
                    userId,
                    EventAction.ADDED,
                    transaction
            ));
        });

        return savedTransaction;
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
    public Page<Transaction> getTransactionsByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        Set<UUID> userIds = new HashSet<>(partyRepository.findPartyMembersPlayerIdsByPlayerId(userId));
        userIds.add(userId);
        return transactionRepository.getTransactionsByPageAndUserIdAndType(query, userIds, type);
    }
}
