package com.fabiankevin.app.services;

import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;

import java.util.List;
import java.util.UUID;

public interface BudgetService {
    BudgetSummary createBudget(CreateBudgetCommand command);

    List<BudgetSummary> getBudgetsByUserId(UUID userId);

    List<BudgetSummary> recreateBudgetsFromLastMonth(UUID userId);

    BudgetSummary patchBudget(PatchBudgetCommand command);

    void deleteBudgetById(UUID id, UUID userId);

    BudgetSummary getBudgetSummaryByIdAndUserId(UUID id, UUID userId);
}
