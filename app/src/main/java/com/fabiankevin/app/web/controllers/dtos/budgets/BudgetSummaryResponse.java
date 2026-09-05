package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a budget summary with spending data")
public record BudgetSummaryResponse(
        @Schema(description = "Unique identifier of the budget", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "User who owns the budget", exampleClasses =  UserResponse.class)
        UserResponse user,
        @Schema(description = "User who last updated the budget",
                exampleClasses = UserResponse.class)
        UserResponse updatedBy,
        @Schema(description = "Timestamp when the budget was last updated", example = "2025-06-15T10:30:00Z")
        Instant updatedAt,
        @Schema(description = "Timestamp when the budget was created", example = "2025-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "Budget period", example = "MONTHLY")
        String period,
        @Schema(description = "Category the budget is for",
                exampleClasses = CategoryResponse.class)
        CategoryResponse category,
        @Schema(description = "Allocated amount", example = "500.0")
        double allocated,
        @Schema(description = "Spent amount", example = "320.50")
        double spent,
        @Schema(description = "Spent percentage", example = "64.1")
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
