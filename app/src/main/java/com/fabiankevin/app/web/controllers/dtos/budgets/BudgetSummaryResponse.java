package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummaryResponse(
        UUID id,
        UUID userId,
        UUID updatedBy,
        Instant updatedAt,
        Instant createdAt,
        String period,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        double allocated,
        double spent,
        double spentPercentage) {

    public static BudgetSummaryResponse from(BudgetSummary summary) {
        Budget budget = summary.budget();
        return BudgetSummaryResponse.builder()
                .id(budget.id())
                .userId(budget.userId())
                .updatedBy(budget.updatedBy())
                .updatedAt(budget.updatedAt())
                .createdAt(budget.createdAt())
                .period(budget.period() != null ? budget.period().name() : null)
                .categoryId(budget.category().id())
                .categoryName(budget.category().name())
                .categoryIcon(budget.category().icon())
                .allocated(budget.allocated())
                .spent(summary.spent())
                .spentPercentage(summary.spentPercentage())
                .build();
    }
}
