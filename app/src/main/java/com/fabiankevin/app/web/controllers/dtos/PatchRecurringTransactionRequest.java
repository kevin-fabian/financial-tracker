package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.recurring_transactions.commands.UpdateRecurringTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to patch a recurring transaction")
public record PatchRecurringTransactionRequest(
        @Schema(description = "Recurring transaction description", example = "Monthly subscription")
        String description,

        @Min(value = 0, message = "amount must be zero or positive")
        @Schema(description = "Amount (must be zero when variableAmount is true)", example = "15.99")
        Double amount,

        @Schema(description = "Whether the amount varies each cycle (no auto-transaction when true)", example = "false")
        Boolean variableAmount,

        @Schema(description = "Category ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,

        @Schema(description = "Account ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,

        @Schema(description = "Whether the recurrence has no end date", example = "false")
        Boolean noEndDate,

        @Min(value = 0, message = "dayOfMonth must be between 0 and 31")
        @Max(value = 31, message = "dayOfMonth must be between 0 and 31")
        @Schema(description = "Day of month when the transaction occurs (0 when noEndDate is true)", example = "15")
        Integer dayOfMonth,

        @Min(value = 1, message = "durationMonths must be at least 1")
        @Schema(description = "Number of months the recurrence lasts", example = "6")
        Integer durationMonths
) {
    public UpdateRecurringTransactionCommand toCommand(UUID userId, UUID id) {
        return UpdateRecurringTransactionCommand.builder()
                .userId(userId)
                .id(id)
                .description(description())
                .amount(amount())
                .variableAmount(variableAmount())
                .categoryId(categoryId())
                .accountId(accountId())
                .noEndDate(noEndDate())
                .dayOfMonth(dayOfMonth())
                .durationMonths(durationMonths())
                .build();
    }
}
