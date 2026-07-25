package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.budgets.Budget;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);
}
