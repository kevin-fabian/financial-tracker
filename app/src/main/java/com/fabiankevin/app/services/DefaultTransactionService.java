package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.TransactionEvent;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.exceptions.DailyTransactionLimitExceededException;
import com.fabiankevin.app.exceptions.InvalidAmountException;
import com.fabiankevin.app.exceptions.TransactionNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.HouseholdRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DefaultTransactionService implements TransactionService {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final Map<SummaryType, SummaryGenerator> generators;
    private final HouseholdRepository householdRepository;
    private final EventPublisher compositeEventPublisher;
    private final int dailyTransactionLimit;
    private final UserClient userClient;

    public DefaultTransactionService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            List<SummaryGenerator> generators,
            HouseholdRepository householdRepository,
            EventPublisher compositeEventPublisher,
            int dailyTransactionLimit,
            UserClient userClient) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.generators = generators.stream()
                .collect(Collectors.toMap(
                        SummaryGenerator::supports,
                        Function.identity()
                ));
        this.householdRepository = householdRepository;
        this.compositeEventPublisher = compositeEventPublisher;
        this.dailyTransactionLimit = dailyTransactionLimit;
        this.userClient = userClient;
    }

    @Transactional
    @Override
    public void deleteTransaction(UUID transactionId, UUID userId) {
        Transaction existing = transactionRepository.findById(transactionId)
                .filter(t -> t.account().user().id().equals(userId))
                .orElseThrow(TransactionNotFoundException::new);

        householdRepository.findByUserId(userId).ifPresent(household ->
            compositeEventPublisher.publish(household.id(), new TransactionEvent(
                    userId,
                    EventAction.DELETED,
                    existing
            ))
        );

        transactionRepository.deleteByIdAndUserId(transactionId, userId);
    }

    @Override
    public TransactionResponse getTransactionById(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.account().user().id().equals(userId))
                .orElseThrow(TransactionNotFoundException::new);

        List<Transaction> enrichedList = enrichWithUserData(List.of(transaction));
        return TransactionResponse.from(enrichedList.getFirst());
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
                .filter(acc -> acc.user().id().equals(userId))
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
                .addedBy(User.of(userId))
                .updatedBy(User.of(userId))
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        householdRepository.findByUserId(userId).ifPresent(household ->
            compositeEventPublisher.publish(household.id(), new TransactionEvent(
                    userId,
                    EventAction.ADDED,
                    savedTransaction
            ))
        );

        return enrichWithUserData(List.of(savedTransaction)).getFirst();
    }

    @Transactional
    @Override
    public Transaction patchTransaction(PatchTransactionCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(TransactionNotFoundException::new);

        if (command.amount() != null && command.amount() <= 0) {
            throw new InvalidAmountException("amount must be greater than zero");
        }

        // validate and fetch new account if provided
        Account newAccount = null;
        if (command.accountId() != null) {
            newAccount = accountRepository.findById(command.accountId())
                    .filter(a -> a.user().id().equals(userId))
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
                .updatedAt(Instant.now())
                .updatedBy(User.of(userId));

        Optional.ofNullable(newAccount).ifPresent(builder::account);
        Optional.ofNullable(command.description()).ifPresent(builder::description);
        Optional.ofNullable(newCategory).ifPresent(builder::category);
        Optional.ofNullable(command.amount()).ifPresent(builder::amount);
        Optional.ofNullable(command.transactionDate()).ifPresent(builder::transactionDate);

        Transaction saved = transactionRepository.save(builder.build());

        householdRepository.findByUserId(userId).ifPresent(household ->
            compositeEventPublisher.publish(household.id(), new TransactionEvent(
                    userId,
                    EventAction.UPDATED,
                    saved
            ))
        );

        return enrichWithUserData(List.of(saved)).getFirst();
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
        Set<UUID> userIds = new HashSet<>(householdRepository.findMembersUserIdsByUserId(userId));
        userIds.add(userId);
        Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, userIds, type);

        return enrichWithUserData(page);
    }

    private Page<Transaction> enrichWithUserData(Page<Transaction> page) {
        List<Transaction> enriched = enrichWithUserData(page.content());

        return new com.fabiankevin.app.models.Page<>(
                enriched,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.last(),
                page.first()
        );
    }

    private List<Transaction> enrichWithUserData(List<Transaction> transactions) {
        Set<UUID> allUserIds = transactions.stream()
                .flatMap(t -> Stream.of(
                        t.addedBy().id(),
                        t.updatedBy().id(),
                        t.account().user().id()
                ))
                .collect(Collectors.toSet());

        if (allUserIds.isEmpty()) {
            return transactions;
        }

        Map<UUID, User> usersById = userClient.getUsersByIds(new ArrayList<>(allUserIds)).stream()
                .collect(Collectors.toMap(User::id, u -> u));

        return transactions.stream()
                .map(t -> {
                    User addedBy = usersById.get(t.addedBy().id());
                    User updatedBy = usersById.get(t.updatedBy().id());
                    User accountUser = usersById.get(t.account().user().id());

                    Transaction.TransactionBuilder builder = t.toBuilder();

                    Optional.ofNullable(addedBy).ifPresent(builder::addedBy);
                    Optional.ofNullable(updatedBy).ifPresent(builder::updatedBy);
                    Optional.ofNullable(accountUser).ifPresent(user ->
                        builder.account(t.account().toBuilder().user(user).build())
                    );

                    return builder.build();
                })
                .toList();
    }
}
