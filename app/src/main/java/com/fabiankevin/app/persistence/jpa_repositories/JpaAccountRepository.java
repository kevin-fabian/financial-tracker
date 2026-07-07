package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.projections.AccountSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {
    Page<AccountEntity> findAllByUserId(UUID userId, Pageable pageable);

    int deleteByIdAndUserId(UUID accountId, UUID userId);

    @Query("""
            SELECT acc.id, acc.name, acc.userId, acc.currency, acc.type, acc.active, acc.system,
                COALESCE(SUM(t.amount), 0.0),
                CAST(COALESCE(COUNT(t.id), 0) AS int)
            FROM AccountEntity acc
            LEFT JOIN TransactionEntity t ON t.account.id = acc.id
            WHERE acc.userId = :userId
            GROUP BY acc
            """)
    Page<AccountSummaryProjection> findAllByUserIdWithSummary(
            @Param("userId") UUID userId,
            Pageable pageable);
}
