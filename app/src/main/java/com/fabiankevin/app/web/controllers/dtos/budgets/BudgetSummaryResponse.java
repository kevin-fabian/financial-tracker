package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummaryResponse(
        UUID id,
        UserResponse user,
        UserResponse updatedBy,
        Instant updatedAt,
        Instant createdAt,
        String period,
        CategoryResponse category,
        double allocated,
        double spent,
        double spentPercentage) {

    public static BudgetSummaryResponse from(BudgetSummary summary) {
        Budget budget = summary.budget();
        return BudgetSummaryResponse.builder()
                .id(budget.id())
                .user(UserResponse.from(budget.user()))
                .updatedBy(UserResponse.from(budget.updatedBy()))
                .updatedAt(budget.updatedAt())
                .createdAt(budget.createdAt())
                .period(budget.period() != null ? budget.period().name() : null)
                .category(CategoryResponse.from(budget.category()))
                .allocated(budget.allocated())
                .spent(summary.spent())
                .spentPercentage(summary.spentPercentage())
                .build();
    }
}
