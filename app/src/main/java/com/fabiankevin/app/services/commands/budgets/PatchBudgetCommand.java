package com.fabiankevin.app.services.commands.budgets;

import com.fabiankevin.app.models.budgets.BudgetPeriod;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchBudgetCommand(
        UUID id,
        UUID categoryId,
        BudgetPeriod period,
        Double allocated,
        UUID userId
) {
}
