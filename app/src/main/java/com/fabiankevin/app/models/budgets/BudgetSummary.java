package com.fabiankevin.app.models.budgets;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummary(
        UUID id,
        UUID userId,
        UUID lastUpdatedBy,
        BudgetPeriod period,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        double allocated,
        double spent,
        double spentPercentage) {
}
