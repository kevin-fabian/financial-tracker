package com.fabiankevin.app.persistence;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
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
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class DefaultRecurringTransactionRepository implements RecurringTransactionRepository {
    private final JpaRecurringTransactionRepository jpaRecurringTransactionRepository;
    private final UserClient userClient;

    @Override
    public RecurringTransaction save(RecurringTransaction recurringTransaction) {
        return jpaRecurringTransactionRepository.save(RecurringTransactionEntity.from(recurringTransaction)).toModel();
    }

    @Override
    public List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, ZonedDateTime now) {
        return jpaRecurringTransactionRepository.findAllSummariesByUserId(userId, now).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Stream<RecurringTransaction> streamDueRecurringTransactions(ZonedDateTime now) {
        return jpaRecurringTransactionRepository.streamDueRecurringTransactions(now)
                .map(RecurringTransactionEntity::toModel);
    }

    private RecurringTransactionSummary toSummary(RecurringTransactionSummaryProjection p) {
        List<User> users = userClient.getUsersByIds(List.of(p.userId()));
        User user = users.isEmpty() ? null : users.getFirst();
        int remainingDays = p.nextOccurrenceDate() != null
                ? (int) ChronoUnit.DAYS.between(ZonedDateTime.now(), p.nextOccurrenceDate())
                : 0;
        return RecurringTransactionSummary.builder()
                .id(p.id())
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
                .remainingDays(remainingDays)
                .transactionStatus(TransactionStatus.valueOf(p.transactionStatus()))
                .status(RecurringTransactionStatus.valueOf(p.status()))
                .user(user)
                .createdAt(p.createdAt())
                .updatedAt(p.updatedAt())
                .build();
    }
}
