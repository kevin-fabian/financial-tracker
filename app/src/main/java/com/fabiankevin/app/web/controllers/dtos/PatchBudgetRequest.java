package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a budget. All fields are optional.")
public record PatchBudgetRequest(
        @Schema(description = "Category identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,
        @Schema(description = "Budget period", example = "MONTHLY")
        BudgetPeriod period,
        @Schema(description = "Allocated amount", example = "500.0")
        Double allocated
) {
    public PatchBudgetCommand toCommand(UUID id, UUID userId) {
        return PatchBudgetCommand.builder()
                .id(id)
                .categoryId(this.categoryId())
                .period(this.period())
                .allocated(this.allocated())
                .userId(userId)
                .build();
    }
}
