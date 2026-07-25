package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a budget record")
public record BudgetResponse(
        @Schema(description = "Unique identifier of the budget", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Budget period", example = "MONTHLY")
        BudgetPeriod period,
        @Schema(description = "Category identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,
        @Schema(description = "Name of the category", example = "GROCERIES")
        String categoryName,
        @Schema(description = "Icon for the category", example = "local_grocery_store")
        String categoryIcon,
        @Schema(description = "Allocated amount", example = "500.0")
        double allocated,
        @Schema(description = "Timestamp when the budget was created")
        Instant createdAt,
        @Schema(description = "Timestamp when the budget was last updated")
        Instant updatedAt
) {
    public static BudgetResponse from(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.id())
                .period(budget.period())
                .categoryId(budget.category().id())
                .categoryName(budget.category().name())
                .categoryIcon(budget.category().icon())
                .allocated(budget.allocated())
                .createdAt(budget.createdAt())
                .updatedAt(budget.updatedAt())
                .build();
    }
}
