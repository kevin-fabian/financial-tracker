package com.fabiankevin.app.models.budgets;

import lombok.Builder;

@Builder(toBuilder = true)
public record BudgetSummary(
        Budget budget,
        double spent,
        double spentPercentage) {
}
