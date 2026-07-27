package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.RecurringTransactionRepository;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class DefaultRecurringTransactionService implements RecurringTransactionService {
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public RecurringTransactionSummary create(CreateRecurringTransactionCommand command) {
        if (command.dayOfMonth() < 1 || command.dayOfMonth() > 31) {
            throw new IllegalArgumentException("dayOfMonth must be between 1 and 31");
        }

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(AccountNotFoundException::new);
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(CategoryNotFoundException::new);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime startDate = now;
        ZonedDateTime endDate = now.plusMonths(command.durationMonths());
        ZonedDateTime nextOccurrenceDate = deriveNextOccurrenceDate(command.dayOfMonth(), now);
        Instant instantNow = now.toInstant();

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .userId(command.userId())
                .description(command.description())
                .amount(command.amount())
                .transactionType(category.type())
                .category(category)
                .account(account)
                .dayOfMonth(command.dayOfMonth())
                .nextOccurrenceDate(nextOccurrenceDate)
                .startDate(startDate)
                .endDate(endDate)
                .status(RecurringTransactionStatus.ACTIVE)
                .createdAt(instantNow)
                .updatedAt(instantNow)
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        TransactionStatus transactionStatus = deriveTransactionStatus(saved.nextOccurrenceDate(), instantNow);

        return RecurringTransactionSummary.builder()
                .id(saved.id())
                .userId(saved.userId())
                .description(saved.description())
                .amount(saved.amount())
                .transactionType(saved.transactionType())
                .category(saved.category())
                .account(saved.account())
                .dayOfMonth(saved.dayOfMonth())
                .nextOccurrenceDate(saved.nextOccurrenceDate())
                .startDate(saved.startDate())
                .endDate(saved.endDate())
                .transactionStatus(transactionStatus)
                .status(saved.status())
                .createdAt(saved.createdAt())
                .updatedAt(saved.updatedAt())
                .build();
    }

    private ZonedDateTime deriveNextOccurrenceDate(int dayOfMonth, ZonedDateTime now) {
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
}
