package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);

    List<BudgetSummary> findAllBudgetSummaryByUserId(List<UUID> usersId);
}
