package com.fabiankevin.app.services;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;

public interface BudgetService {
    Budget createBudget(CreateBudgetCommand command);
}
