package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.projections.AccountSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {
    Page<AccountEntity> findAllByUserId(UUID userId, Pageable pageable);

    Optional<AccountEntity> findByNameAndTypeAndUserId(String name, String type, UUID userId);

    int deleteByIdAndUserId(UUID accountId, UUID userId);

    long deleteAllByUserId(UUID userId);

    @Query("""
            SELECT acc,
                COALESCE(SUM(CASE WHEN t.category.transactionType = TransactionType.INCOME THEN t.amount ELSE -t.amount END), 0.0) AS totalBalance,
                CAST(COALESCE(COUNT(t.id), 0) AS int)
            FROM AccountEntity acc
            LEFT JOIN TransactionEntity t ON t.account.id = acc.id
                AND t.transactionDate BETWEEN :monthStart AND :monthEnd
                AND t.account.userId IN (:userIds)
            LEFT JOIN t.category
            WHERE acc.userId IN (:userIds)
            GROUP BY acc
            """)
    Page<AccountSummaryProjection> findAllByUserIdsWithSummary(
            @Param("userIds") List<UUID> userIds,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            Pageable pageable);

    @Query("""
            SELECT acc,
                COALESCE(SUM(CASE WHEN t.category.transactionType = TransactionType.INCOME THEN t.amount ELSE -t.amount END), 0.0),
                CAST(COALESCE(COUNT(t.id), 0) AS int)
            FROM AccountEntity acc
            LEFT JOIN TransactionEntity t ON t.account.id = acc.id
                AND t.account.userId = :userId
            LEFT JOIN t.category
            WHERE acc.id = :accountId
                AND acc.userId = :userId
            GROUP BY acc
            """)
    Optional<AccountSummaryProjection> findSummaryByIdAndUserId(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId);
}
