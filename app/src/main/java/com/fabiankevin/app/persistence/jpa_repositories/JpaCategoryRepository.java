package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.projections.CategorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByNameAndTransactionTypeAndUserId(String name, TransactionType type, UUID userId);
    int deleteByIdAndUserId(UUID id, UUID userId);
    Page<CategoryEntity> findAllByUserId(UUID userId, Pageable pageable);
    Page<CategoryEntity> findAllByUserIdAndTransactionType(UUID userId, TransactionType type, Pageable pageable);
    Optional<CategoryEntity> findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(String name, TransactionType type, UUID userId);
    @Query("""
            SELECT new com.fabiankevin.app.persistence.projections.CategorySummaryProjection(
                c.id, c.name, c.transactionType, c.userId, c.icon, c.active, c.system,
                SUM(t.amount.amount),
                CAST(COUNT(t.id) AS int)
            )
            FROM CategoryEntity c
            LEFT JOIN TransactionEntity t ON t.category.id = c.id
            WHERE c.userId = :userId
            GROUP BY c
            """)
    List<CategorySummaryProjection> findByUserIdWithSummary(@Param("userId") UUID userId);
}
