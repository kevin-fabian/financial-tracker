package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.RecurringTransactionSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaRecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
    @Query("""
            SELECT rt.id AS id, rt.userId AS userId, rt.description AS description, rt.amount AS amount,
                rt.dayOfMonth AS dayOfMonth,
                rt.nextOccurrenceDate AS nextOccurrenceDate, rt.startDate AS startDate, rt.endDate AS endDate,
                CASE
                    WHEN rt.nextOccurrenceDate > :now THEN 'UPCOMING'
                    WHEN (SELECT COUNT(t.id) FROM com.fabiankevin.app.persistence.entities.TransactionEntity t WHERE t.recurringTransactionId = rt.id) > 0 THEN 'PAID'
                    ELSE 'OVERDUE'
                END AS transactionStatus,
                STR(rt.status) AS status, rt.createdAt AS createdAt, rt.updatedAt AS updatedAt,
                rt.category.id AS categoryId, rt.category.name AS categoryName,
                STR(rt.category.transactionType) AS categoryType, rt.category.userId AS categoryUserId,
                rt.category.icon AS categoryIcon, rt.category.active AS categoryActive,
                rt.category.createdAt AS categoryCreatedAt, rt.category.updatedAt AS categoryUpdatedAt,
                rt.account.id AS accountId, rt.account.name AS accountName,
                rt.account.userId AS accountUserId, rt.account.currency AS accountCurrency,
                rt.account.type AS accountType, rt.account.active AS accountActive,
                rt.account.createdAt AS accountCreatedAt, rt.account.updatedAt AS accountUpdatedAt
            FROM RecurringTransactionEntity rt
            WHERE rt.userId = :userId
            """)
    List<RecurringTransactionSummaryProjection> findAllSummariesByUserId(@Param("userId") UUID userId, @Param("now") ZonedDateTime now);
}
