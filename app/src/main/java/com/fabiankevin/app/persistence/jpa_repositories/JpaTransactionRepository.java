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
                SELECT t.category.name AS label, SUM(t.amountValue) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY t.category.name
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByCategory(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT MONTH(t.transactionDate) AS label, SUM(t.amountValue) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY MONTH(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByMonth(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT YEAR(t.transactionDate) AS label, SUM(t.amountValue) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY YEAR(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByYear(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT DAY(t.transactionDate) AS label, SUM(t.amountValue) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY DAY(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByDay(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") List<UUID> userIds,
            @Param("type") TransactionType type);

    Page<TransactionEntity> findAllByAccountUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE t.account.userId = :userId
              AND (:type IS NULL OR t.category.transactionType = :type)
            """)
    Page<TransactionEntity> findAllByAccountUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            Pageable pageable);

    int deleteByIdAndAccountUserId(UUID id, UUID userId);

    @Query("""
            SELECT STR(t.category.transactionType) as label, COALESCE(SUM(t.amountValue), 0.0) as total
            FROM TransactionEntity t
            WHERE t.account.userId = :userId
              AND t.transactionDate BETWEEN :from AND :to
              AND (:accountId IS NULL OR t.account.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            GROUP BY t.category.transactionType
            """)
    Streamable<SummaryPointProjection> sumByTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId);

    @Query("""
            SELECT SUM(CASE WHEN t.category.transactionType = com.fabiankevin.app.models.enums.TransactionType.INCOME THEN t.amountValue ELSE -t.amountValue END)
            FROM TransactionEntity t
            WHERE t.account.userId = :userId
              AND (:accountId IS NULL OR t.account.id = :accountId)
            """)
    double sumBalance(
            @Param("userId") UUID userId,
            @Param("accountId") java.util.UUID accountId);
}
