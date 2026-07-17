package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.projections.CategorySummaryProjection;
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

    long deleteAllByUserId(UUID userId);

    Page<CategoryEntity> findAllByUserId(UUID userId, Pageable pageable);
    Page<CategoryEntity> findAllByUserIdAndTransactionType(UUID userId, TransactionType type, Pageable pageable);
    Optional<CategoryEntity> findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(String name, TransactionType type, UUID userId);

    List<CategoryEntity> findAllByNameIn(List<String> names);
    @Query("""
            SELECT c.id, c.name, c.transactionType, c.userId, c.icon, c.active, c.system,
                COALESCE(SUM(t.amount), 0.0),
                CAST(COALESCE(COUNT(t.id), 0) AS int)
            FROM CategoryEntity c
            LEFT JOIN TransactionEntity t ON t.category.id = c.id
                AND t.transactionDate >= :monthStart
                AND t.transactionDate <= :monthEnd
            WHERE c.userId = :userId
            AND (:type IS NULL OR c.transactionType = :type)
            GROUP BY c
            """)
    Page<CategorySummaryProjection> findAllByUserIdAndTransactionTypeWithSummary(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("monthStart") java.time.LocalDate monthStart,
            @Param("monthEnd") java.time.LocalDate monthEnd,
            Pageable pageable);
}
