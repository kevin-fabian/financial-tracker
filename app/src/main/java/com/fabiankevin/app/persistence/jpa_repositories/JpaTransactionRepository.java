package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query("""
                SELECT t.category.name AS label, COALESCE(SUM(t.amount), 0.0) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY t.category.name
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByCategory(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") Set<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT MONTH(t.transactionDate) AS label, COALESCE(SUM(t.amount), 0.0) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY MONTH(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByMonth(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") Set<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT YEAR(t.transactionDate) AS label, COALESCE(SUM(t.amount), 0.0) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY YEAR(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByYear(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") Set<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
                SELECT DAY(t.transactionDate) AS label,
                       COALESCE(SUM(
                           CASE
                               WHEN :type IS NULL THEN
                                   CASE WHEN t.category.transactionType = TransactionType.INCOME THEN t.amount ELSE -t.amount END
                               ELSE t.amount
                           END
                       ), 0.0) AS sum
                FROM TransactionEntity t
                WHERE t.transactionDate BETWEEN :from AND :to
                  AND t.account.userId IN :userIds
                  AND (:type IS NULL OR t.category.transactionType = :type)
                GROUP BY DAY(t.transactionDate)
            """)
    Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByDay(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userIds") Set<UUID> userIds,
            @Param("type") TransactionType type);

    @Query("""
            SELECT t FROM TransactionEntity t
            JOIN FETCH t.account a
            JOIN FETCH t.category c
            WHERE a.userId IN :userIds
              AND (:type IS NULL OR c.transactionType = :type)
            """)
    Page<TransactionEntity> findAllByUserIdsAndType(
            @Param("userIds") Set<UUID> userIds,
            @Param("type") TransactionType type,
            Pageable pageable);

    int deleteByIdAndAccountUserId(UUID id, UUID userId);

    @Query("""
            SELECT STR(t.category.transactionType) as label, COALESCE(SUM(t.amount), 0.0) as total
            FROM TransactionEntity t
            WHERE t.account.userId IN :userIds
              AND t.transactionDate BETWEEN :from AND :to
              AND (:accountId IS NULL OR t.account.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            GROUP BY t.category.transactionType
            """)
    Streamable<SummaryPointProjection> sumByTypeAndDateRange(
            @Param("userIds") Set<UUID> userIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId);

    @Query("""
            SELECT STR(t.category.transactionType) as label, COALESCE(SUM(t.amount), 0.0) as total
            FROM TransactionEntity t
            WHERE t.account.userId IN :userIds
              AND t.transactionDate BETWEEN :from AND :to
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            GROUP BY t.category.transactionType
            """)
    Streamable<SummaryPointProjection> sumByTypeAndDateRangeByCategory(
            @Param("userIds") Set<UUID> userIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("categoryId") UUID categoryId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.category.transactionType = TransactionType.INCOME THEN t.amount ELSE -t.amount END), 0.0)
            FROM TransactionEntity t
            WHERE t.account.userId IN :userIds
              AND t.transactionDate BETWEEN :from AND :to
            """)
    double sumBalance(
            @Param("userIds") Set<UUID> userIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.category.transactionType = TransactionType.INCOME THEN t.amount ELSE -t.amount END), 0.0)
            FROM TransactionEntity t
            WHERE t.account.userId IN :userIds
            """)
    double sumBalance(@Param("userIds") Set<UUID> userIds);

    @Query("""
            SELECT CAST(t.account.userId AS string) AS label,
                   COUNT(t) / 7.0 AS total
            FROM TransactionEntity t
            WHERE t.account.userId IN :userIds
              AND t.transactionDate >= :startDate
            GROUP BY t.account.userId
            """)
    Streamable<SummaryPointProjection> getDailyAveragePastWeek(
            @Param("userIds") Set<UUID> userIds,
            @Param("startDate") LocalDate startDate);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0.0)
            FROM TransactionEntity t
            WHERE t.category.id = :categoryId
              AND t.account.userId = :userId
            """)
    double sumSpentByCategoryIdAndUserId(@Param("categoryId") UUID categoryId, @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"account", "category"})
    Optional<TransactionEntity> findByRecurringTransactionId(UUID recurringTransactionId);

    @EntityGraph(attributePaths = {"account", "category"})
    Optional<TransactionEntity> findByIdAndAccountUserId(UUID id, UUID userId);

    @Query("""
            SELECT COUNT(t)
            FROM TransactionEntity t
            WHERE t.category.id = :categoryId
              AND t.account.userId = :userId
            """)
    long countByCategoryIdAndUserId(@Param("categoryId") UUID categoryId, @Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(t)
            FROM TransactionEntity t
            WHERE t.account.userId = :userId
              AND t.createdAt >= :startInclusive
              AND t.createdAt < :endExclusive
            """)
    long countByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                          @Param("startInclusive") Instant startInclusive,
                                          @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COUNT(t)
            FROM TransactionEntity t
            WHERE t.account.id = :accountId
            """)
    long countByAccountId(@Param("accountId") UUID accountId);

    void deleteAllByCategoryId(UUID categoryId);
}
