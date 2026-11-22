package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a budget")
public record CreateBudgetRequest(
        @Schema(description = "Client-provided UUID for the budget. If not provided, a server-generated UUID will be used", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @NotNull(message = "period is required")
        @Schema(description = "Budget period", example = "MONTHLY")
        BudgetPeriod period,
        @NotNull(message = "categoryId is required")
        @Schema(description = "Category identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,
        @Positive(message = "allocated must be positive")
        @Schema(description = "Allocated amount", example = "500.0")
        double allocated
) {
    public CreateBudgetCommand toCommand(UUID userId) {
        return CreateBudgetCommand.builder()
                .id(this.id)
                .userId(userId)
                .period(this.period)
                .categoryId(this.categoryId)
                .allocated(this.allocated)
                .build();
    }
}
