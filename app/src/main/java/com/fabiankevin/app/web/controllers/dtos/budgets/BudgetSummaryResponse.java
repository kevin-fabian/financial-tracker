package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a budget with aggregated spending data")
public record BudgetSummaryResponse(
        @Schema(description = "Unique identifier of the budget", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Identifier of the user who last updated the budget", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID lastUpdatedBy,
        @Schema(description = "Full name of the user who last updated the budget", example = "John Doe")
        String lastUpdatedByName,
        @Schema(description = "First name of the user who last updated the budget", example = "John")
        String firstName,
        @Schema(description = "Last name of the user who last updated the budget", example = "Doe")
        String lastName,
        @Schema(description = "Initials of the user who last updated the budget", example = "JD")
        String initial,
        @Schema(description = "Timestamp of the last update", example = "2026-07-26T09:00:00Z")
        Instant updatedAt,
        @Schema(description = "Timestamp when the budget was created", example = "2026-07-26T09:00:00Z")
        Instant createdAt,
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
        @Schema(description = "Amount spent against the budget", example = "200.0")
        double spent,
        @Schema(description = "Percentage of allocation spent", example = "40.0")
        double spentPercentage
) {
    public static BudgetSummaryResponse from(BudgetSummary summary) {
        return BudgetSummaryResponse.builder()
                .id(summary.id())
                .lastUpdatedBy(summary.lastUpdatedBy())
                .lastUpdatedByName(summary.lastUpdatedByName())
                .firstName(summary.firstName())
                .lastName(summary.lastName())
                .initial(summary.initial())
                .updatedAt(summary.updatedAt())
                .createdAt(summary.createdAt())
                .period(summary.period())
                .categoryId(summary.categoryId())
                .categoryName(summary.categoryName())
                .categoryIcon(summary.categoryIcon())
                .allocated(summary.allocated())
                .spent(summary.spent())
                .spentPercentage(summary.spentPercentage())
                .build();
    }
}
