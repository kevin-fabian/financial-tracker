package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.RecurringTransactionSummaryProjection;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface JpaRecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
    @Query("""
            SELECT rt.id AS id,rt.description AS description, rt.amount AS amount,
                rt.dayOfMonth AS dayOfMonth,
                rt.nextOccurrenceDate AS nextOccurrenceDate, rt.endDate AS endDate,
                CASE
                    WHEN rt.nextOccurrenceDate > :now THEN 'UPCOMING'
                    WHEN (SELECT COUNT(t.id) FROM com.fabiankevin.app.persistence.entities.TransactionEntity t WHERE t.recurringTransactionId = rt.id) > 0 THEN 'PAID'
                    ELSE 'OVERDUE'
                END AS transactionStatus,
                STR(rt.status) AS status,
                rt.updatedBy AS updatedById,
                rt.createdAt AS createdAt,
                rt.updatedAt AS updatedAt,
                rt.category,
                rt.account
            FROM RecurringTransactionEntity rt
            WHERE rt.account.userId = :userId
            """)
    List<RecurringTransactionSummaryProjection> findAllSummariesByUserId(@Param("userId") UUID userId, @Param("now") LocalDate now);

    @EntityGraph(attributePaths = {"account", "category"})
    @Query("""
            SELECT rt FROM RecurringTransactionEntity rt
            WHERE rt.nextOccurrenceDate < :now
              AND rt.variableAmount = false
              AND rt.status = 'ACTIVE'
            """)
    Stream<RecurringTransactionEntity> streamDueRecurringTransactions(@Param("now") LocalDate now);

    @EntityGraph(attributePaths = {"account", "category"})
    Optional<RecurringTransactionEntity> findByIdAndAccountUserId(UUID id, UUID userId);

    int deleteByIdAndAccountUserId(UUID id, UUID userId);
}
