package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query("""
                SELECT t.category.name AS category, SUM(t.amount.amount) AS sum
                FROM TransactionEntity t
                WHERE YEAR(t.transactionDate) = :year and t.account.userId IN :userIds
                GROUP BY t.category.name
            """)
    Streamable<SummaryPointProjection> getSummaryByYearAndUserIdGroupedByCategory(@Param("year") int year, @Param("userIds") List<UUID> userIds);
}
