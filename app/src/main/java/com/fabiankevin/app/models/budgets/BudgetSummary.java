package com.fabiankevin.app.models.budgets;

import com.fabiankevin.app.models.Category;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummary(
        UUID id,
        UUID userId,
        UUID lastUpdatedBy,
        BudgetPeriod period,
        Category category,
        String icon,
        double allocated,
        double spent,
        double spentPercentage,
        Instant createdAt,
        Instant updatedAt) {
}
