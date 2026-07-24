package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.projections.AccountSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {
    Page<AccountEntity> findAllByUserId(UUID userId, Pageable pageable);

    List<AccountEntity> findAllByNameIn(List<String> accountNames);

    Optional<AccountEntity> findByNameAndTypeAndUserId(String name, String type, UUID userId);

    int deleteByIdAndUserId(UUID accountId, UUID userId);

    long deleteAllByUserId(UUID userId);

    @Query("""
            SELECT acc.id, acc.name, acc.userId, acc.currency, acc.type, acc.active, acc.system,
                COALESCE(SUM(CASE WHEN t.category.transactionType = com.fabiankevin.app.models.enums.TransactionType.INCOME THEN t.amount ELSE -t.amount END), 0.0) AS totalBalance,
                CAST(COALESCE(COUNT(t.id), 0) AS int)
            FROM AccountEntity acc
            LEFT JOIN TransactionEntity t ON t.account.id = acc.id
            LEFT JOIN t.category
            WHERE acc.userId = :userId
            GROUP BY acc
            """)
    Page<AccountSummaryProjection> findAllByUserIdWithSummary(
            @Param("userId") UUID userId,
            Pageable pageable);
}
