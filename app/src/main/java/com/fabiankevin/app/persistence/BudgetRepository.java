package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);

    boolean existsByCategoryIdAndUserIdAndCreatedAtBetween(UUID categoryId, UUID userId, Instant startInclusive, Instant endExclusive);

    int deleteByIdAndUserId(UUID id, UUID userId);

    List<Budget> findAllByUserIdAndCreatedAtBetween(UUID userId, Instant startInclusive, Instant endExclusive);

    List<BudgetSummary> findAllBudgetSummaryByUserId(List<UUID> usersId, LocalDate startMonth, LocalDate endMonth);

    Optional<BudgetSummary> findBudgetSummaryById(UUID id);
}
