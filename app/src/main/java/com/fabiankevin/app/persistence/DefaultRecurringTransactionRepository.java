package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.RecurringTransactionSummaryProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class DefaultRecurringTransactionRepository implements RecurringTransactionRepository {
    private final JpaRecurringTransactionRepository jpaRecurringTransactionRepository;

    @Override
    public RecurringTransaction save(RecurringTransaction recurringTransaction) {
        return jpaRecurringTransactionRepository.save(RecurringTransactionEntity.from(recurringTransaction)).toModel();
    }

    @Override
    public List<RecurringTransaction> saveAll(List<RecurringTransaction> recurringTransactions) {
        List<RecurringTransactionEntity> entities = recurringTransactions.stream()
                .map(RecurringTransactionEntity::from)
                .toList();
        return jpaRecurringTransactionRepository.saveAll(entities).stream()
                .map(RecurringTransactionEntity::toModel)
                .toList();
    }

    @Override
    public List<RecurringTransactionSummary> findSummariesByUserId(UUID userId, LocalDate now) {
        return jpaRecurringTransactionRepository.findAllSummariesByUserId(userId, now).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<RecurringTransactionSummary> findSummariesByUserIds(List<UUID> userIds, LocalDate now) {
        return userIds.stream()
                .flatMap(userId -> jpaRecurringTransactionRepository.findAllSummariesByUserId(userId, now).stream())
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Stream<RecurringTransaction> streamDueRecurringTransactions(LocalDate now) {
        return jpaRecurringTransactionRepository.streamDueRecurringTransactions(now)
                .map(RecurringTransactionEntity::toModel);
    }

    @Override
    public int deleteByIdAndUserId(UUID id, UUID userId) {
        return jpaRecurringTransactionRepository.deleteByIdAndAccountUserId(id, userId);
    }

    @Override
    public Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRecurringTransactionRepository.findByIdAndAccountUserId(id, userId)
                .map(RecurringTransactionEntity::toModel);
    }

    private RecurringTransactionSummary toSummary(RecurringTransactionSummaryProjection p) {
        return RecurringTransactionSummary.builder()
                .id(p.id())
                .description(p.description())
                .amount(p.amount())
                .category(p.category().toModel())
                .account(p.account().toModel())
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
