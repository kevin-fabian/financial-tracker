package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query("""
                SELECT t.category.name AS label, SUM(t.amount.amount) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.type = :type)
                GROUP BY t.category.name
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByCategory(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT MONTH(t.transactionDate) AS label, SUM(t.amount.amount) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.type = :type)
                GROUP BY MONTH(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByMonth(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT YEAR(t.transactionDate) AS label, SUM(t.amount.amount) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.type = :type)
                GROUP BY YEAR(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByYear(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT DAY(t.transactionDate) AS label, SUM(t.amount.amount) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.type = :type)
                GROUP BY DAY(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByDay(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    // Pageable lookup for transactions by account.userId
    Page<TransactionEntity> findAllByAccountUserId(UUID userId, Pageable pageable);

    // Delete a transaction by id only if it belongs to the specified user id (account.userId)
    // Return number of rows deleted
    int deleteByIdAndAccountUserId(UUID id, UUID userId);
}
