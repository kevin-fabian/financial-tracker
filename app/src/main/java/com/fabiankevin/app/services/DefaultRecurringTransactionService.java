package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.*;
import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.RecurringTransactionRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import com.fabiankevin.app.services.recurring_transactions.commands.UpdateRecurringTransactionCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DefaultRecurringTransactionService implements RecurringTransactionService {
    private static final int BATCH_SIZE = 50;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;

    @Override
    public RecurringTransactionSummary create(CreateRecurringTransactionCommand command) {
        if (command.variableAmount() && command.amount() != 0) {
            throw new InvalidAmountException("amount must be zero when variableAmount is true");
        }
        if (!command.noEndDate() && (command.dayOfMonth() < 1 || command.dayOfMonth() > 31)) {
            throw new IllegalArgumentException("dayOfMonth must be between 1 and 31");
        }

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(AccountNotFoundException::new);
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(CategoryNotFoundException::new);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime endDate = command.noEndDate() ? null : now.plusMonths(command.durationMonths());
        ZonedDateTime nextOccurrenceDate = deriveNextOccurrenceDate(command.dayOfMonth(), now);
        Instant instantNow = now.toInstant();

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .userId(command.userId())
                .description(command.description())
                .amount(command.amount())
                .variableAmount(command.variableAmount())
                .category(category)
                .account(account)
                .dayOfMonth(command.dayOfMonth())
                .nextOccurrenceDate(nextOccurrenceDate)
                .endDate(endDate)
                .status(RecurringTransactionStatus.ACTIVE)
                .createdAt(instantNow)
                .updatedAt(instantNow)
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        User user = userClient.getUsersByIds(List.of(command.userId())).stream().findFirst().orElse(null);
        int remainingDays = getRemainingDays(saved.nextOccurrenceDate(), now);

        TransactionStatus transactionStatus = deriveTransactionStatus(saved.nextOccurrenceDate(), instantNow);

        return RecurringTransactionSummary.builder()
                .id(saved.id())
                .description(saved.description())
                .amount(saved.amount())
                .variableAmount(saved.variableAmount())
                .category(saved.category())
                .account(saved.account())
                .dayOfMonth(saved.dayOfMonth())
                .nextOccurrenceDate(saved.nextOccurrenceDate())
                .endDate(saved.endDate())
                .remainingDays(remainingDays)
                .transactionStatus(transactionStatus)
                .status(saved.status())
                .user(user)
                .createdAt(saved.createdAt())
                .updatedAt(saved.updatedAt())
                .build();
    }

    @Override
    public List<RecurringTransactionSummary> getRecurringTransactionsByUserId(UUID userId) {
        ZonedDateTime now = ZonedDateTime.now();
        List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(userId, now);
        User user = userClient.getUsersByIds(List.of(userId)).stream().findFirst().orElse(null);
        return summaries.stream()
                .map(s -> {
                    int remainingDays = getRemainingDays(s.nextOccurrenceDate(), now);
                    return s.toBuilder()
                            .remainingDays(remainingDays)
                            .user(user)
                            .build();
                })
                .toList();
    }

    @Transactional
    @Override
    public void deleteRecurringTransactionById(UUID id, UUID userId) {
        int deleted = recurringTransactionRepository.deleteByIdAndUserId(id, userId);
        if (deleted == 0) {
            throw new NotFoundException("Recurring transaction not found");
        }
    }

    @Transactional
    @Override
    public RecurringTransactionSummary updateRecurringTransaction(UpdateRecurringTransactionCommand command) {
        RecurringTransaction existing = recurringTransactionRepository.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found"));

        boolean newVariableAmount = Optional.ofNullable(command.variableAmount()).orElse(existing.variableAmount());
        double newAmount = Optional.ofNullable(command.amount()).orElse(existing.amount());
        if (newVariableAmount && newAmount != 0) {
            throw new InvalidAmountException("amount must be zero when variableAmount is true");
        }

        Category category = Optional.ofNullable(command.categoryId())
                .map(id -> categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new))
                .orElse(existing.category());

        Account account = Optional.ofNullable(command.accountId())
                .map(id -> accountRepository.findById(id).orElseThrow(AccountNotFoundException::new))
                .orElse(existing.account());

        boolean noEndDate = Optional.ofNullable(command.noEndDate()).orElse(existing.endDate() == null);
        int dayOfMonth = Optional.ofNullable(command.dayOfMonth()).orElse(existing.dayOfMonth());
        int durationMonths = Optional.ofNullable(command.durationMonths()).orElse(0);

        if (!noEndDate && command.durationMonths() == null && existing.endDate() == null) {
            throw new InvalidDurationException("durationMonths is required when noEndDate is false and no existing endDate");
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime endDate;
        if (noEndDate) {
            endDate = null;
        } else if (command.durationMonths() != null) {
            endDate = now.plusMonths(durationMonths);
        } else {
            endDate = existing.endDate();
        }

        ZonedDateTime nextOccurrenceDate = Optional.ofNullable(command.dayOfMonth())
                .map(dm -> deriveNextOccurrenceDate(dm, now))
                .orElse(existing.nextOccurrenceDate());

        RecurringTransaction updated = existing.toBuilder()
                .description(Optional.ofNullable(command.description()).orElse(existing.description()))
                .amount(newAmount)
                .variableAmount(newVariableAmount)
                .category(category)
                .account(account)
                .dayOfMonth(dayOfMonth)
                .nextOccurrenceDate(nextOccurrenceDate)
                .endDate(endDate)
                .updatedAt(now.toInstant())
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(updated);

        User user = userClient.getUsersByIds(List.of(command.userId())).stream().findFirst().orElse(null);
        int remainingDays = (int) ChronoUnit.DAYS.between(now, saved.nextOccurrenceDate());
        TransactionStatus transactionStatus = deriveTransactionStatus(saved.nextOccurrenceDate(), now.toInstant());

        return RecurringTransactionSummary.builder()
                .id(saved.id())
                .description(saved.description())
                .amount(saved.amount())
                .variableAmount(saved.variableAmount())
                .category(saved.category())
                .account(saved.account())
                .dayOfMonth(saved.dayOfMonth())
                .nextOccurrenceDate(saved.nextOccurrenceDate())
                .endDate(saved.endDate())
                .remainingDays(remainingDays)
                .transactionStatus(transactionStatus)
                .status(saved.status())
                .user(user)
                .createdAt(saved.createdAt())
                .updatedAt(saved.updatedAt())
                .build();
    }

    private ZonedDateTime deriveNextOccurrenceDate(int dayOfMonth, ZonedDateTime now) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            return now.plusMonths(1);
        }
        if (now.getDayOfMonth() < dayOfMonth) {
            return now.withDayOfMonth(dayOfMonth);
        }
        return now.plusMonths(1).withDayOfMonth(dayOfMonth);
    }

    private TransactionStatus deriveTransactionStatus(ZonedDateTime nextOccurrenceDate, Instant now) {
        if (nextOccurrenceDate.toInstant().isAfter(now)) {
            return TransactionStatus.UPCOMING;
        }
        return TransactionStatus.OVERDUE;
    }

    @Async
    @Transactional
    @Override
    public void processDueRecurringTransactions() {
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = LocalDate.now();
        Instant instantNow = now.toInstant();
        List<Transaction> batch = new ArrayList<>(BATCH_SIZE);

        try (Stream<RecurringTransaction> stream = recurringTransactionRepository.streamDueRecurringTransactions(now)) {
            stream.forEach(recurring -> {
                Transaction transaction = Transaction.builder()
                        .account(recurring.account())
                        .category(recurring.category())
                        .type(recurring.category().type())
                        .amount(Amount.of(recurring.amount(), recurring.account().currency()))
                        .description(recurring.description())
                        .transactionDate(today)
                        .recurringTransactionId(recurring.id())
                        .createdAt(instantNow)
                        .updatedAt(instantNow)
                        .build();
                batch.add(transaction);

                if (batch.size() >= BATCH_SIZE) {
                    transactionRepository.saveAll(new ArrayList<>(batch));
                    batch.clear();
                    transactionRepository.flush();
                }
            });
        }

        if (!batch.isEmpty()) {
            transactionRepository.saveAll(batch);
            transactionRepository.flush();
        }
    }

    private static int getRemainingDays(ZonedDateTime nextOccurrenceDate, ZonedDateTime now) {
        return nextOccurrenceDate != null
                ? (int) ChronoUnit.DAYS.between(now, nextOccurrenceDate)
                : 0;
    }
}
