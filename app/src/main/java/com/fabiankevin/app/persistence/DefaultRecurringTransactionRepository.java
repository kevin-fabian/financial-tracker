package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.RecurringTransactionSummaryProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DefaultRecurringTransactionRepository implements RecurringTransactionRepository {
    private final JpaRecurringTransactionRepository jpaRecurringTransactionRepository;

    @Override
    public RecurringTransaction save(RecurringTransaction recurringTransaction) {
        return jpaRecurringTransactionRepository.save(RecurringTransactionEntity.from(recurringTransaction)).toModel();
    }

    @Override
    public List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, ZonedDateTime now) {
        return jpaRecurringTransactionRepository.findAllSummariesByUserId(userId, now).stream()
                .map(DefaultRecurringTransactionRepository::toSummary)
                .toList();
    }

    private static RecurringTransactionSummary toSummary(RecurringTransactionSummaryProjection p) {
        return RecurringTransactionSummary.builder()
                .id(p.id())
                .userId(p.userId())
                .description(p.description())
                .amount(p.amount())
                .category(Category.builder()
                        .id(p.categoryId())
                        .name(p.categoryName())
                        .type(TransactionType.valueOf(p.categoryType()))
                        .userId(p.categoryUserId())
                        .icon(p.categoryIcon())
                        .active(p.categoryActive())
                        .createdAt(p.categoryCreatedAt())
                        .updatedAt(p.categoryUpdatedAt())
                        .build())
                .account(Account.builder()
                        .id(p.accountId())
                        .name(p.accountName())
                        .userId(p.accountUserId())
                        .currency(Currency.getInstance(p.accountCurrency()))
                        .type(AccountType.valueOf(p.accountType()))
                        .active(p.accountActive())
                        .createdAt(p.accountCreatedAt())
                        .updatedAt(p.accountUpdatedAt())
                        .build())
                .dayOfMonth(p.dayOfMonth())
                .nextOccurrenceDate(p.nextOccurrenceDate())
                .endDate(p.endDate())
                .transactionStatus(TransactionStatus.valueOf(p.transactionStatus()))
                .status(RecurringTransactionStatus.valueOf(p.status()))
                .createdAt(p.createdAt())
                .updatedAt(p.updatedAt())
                .build();
    }
}
