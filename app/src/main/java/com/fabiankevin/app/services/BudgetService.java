package com.fabiankevin.app.services;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;

import java.util.List;
import java.util.UUID;

public interface BudgetService {
    Budget createBudget(CreateBudgetCommand command);

    List<BudgetSummary> getBudgetsByUserId(UUID userId);

    Budget patchBudget(PatchBudgetCommand command);
}
