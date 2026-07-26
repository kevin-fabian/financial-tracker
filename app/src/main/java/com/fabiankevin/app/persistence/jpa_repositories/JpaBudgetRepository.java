package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.BudgetEntity;
import com.fabiankevin.app.persistence.entities.projections.BudgetSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    Optional<BudgetEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);

    boolean existsByCategoryIdAndUserIdAndCreatedAtBetween(UUID categoryId, UUID userId, Instant startInclusive, Instant endExclusive);

    int deleteByIdAndUserId(UUID id, UUID userId);

    @Query("""
                SELECT b FROM BudgetEntity b
                WHERE b.userId = :userId
                  AND b.createdAt >= :startInclusive
                  AND b.createdAt < :endExclusive
            """)
    List<BudgetEntity> findAllByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                          @Param("startInclusive") Instant startInclusive,
                                                          @Param("endExclusive") Instant endExclusive);

    @Query("""
                SELECT b.id, b.userId, b.lastUpdatedBy, b.updatedAt, b.createdAt, STR(b.period), b.allocated as allocated,
                       c.id as categoryId, c.name as categoryName, c.icon as categoryIcon,
                       COALESCE(SUM(t.amount), 0) AS spent
                FROM BudgetEntity b
                JOIN b.category c
                LEFT JOIN TransactionEntity t ON t.category.id = c.id
                    AND YEAR(t.transactionDate) = YEAR(b.createdAt)
                    AND MONTH(t.transactionDate) = MONTH(b.createdAt)
                WHERE b.userId IN :userIds
                GROUP BY b.id, c.id
            """)
    Streamable<BudgetSummaryProjection> findAllBudgetSummaryByUserIds(@Param("userIds") List<UUID> userIds);
}
