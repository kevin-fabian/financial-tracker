package com.fabiankevin.app.services.commands.budgets;

import com.fabiankevin.app.models.budgets.BudgetPeriod;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateBudgetCommand(
        UUID userId,
        BudgetPeriod period,
        UUID categoryId,
        String icon,
        double allocated) {
}
