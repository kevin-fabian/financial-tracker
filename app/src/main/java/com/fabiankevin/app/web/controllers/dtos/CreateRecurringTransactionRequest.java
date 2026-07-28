package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a recurring transaction")
public record CreateRecurringTransactionRequest(
        @NotBlank(message = "description is required")
        @Schema(description = "Recurring transaction description", example = "Monthly subscription")
        String description,

        @Min(value = 0, message = "amount must be zero or positive")
        @Schema(description = "Amount (must be zero when variableAmount is true)", example = "15.99")
        double amount,

        @Schema(description = "Whether the amount varies each cycle (no auto-transaction when true)", example = "false")
        boolean variableAmount,

        @NotNull(message = "categoryId is required")
        @Schema(description = "Category ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,

        @NotNull(message = "accountId is required")
        @Schema(description = "Account ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,

        @Schema(description = "Whether the recurrence has no end date", example = "false")
        boolean noEndDate,

        @Min(value = 0, message = "dayOfMonth must be between 0 and 31")
        @Max(value = 31, message = "dayOfMonth must be between 0 and 31")
        @Schema(description = "Day of month when the transaction occurs (0 when noEndDate is true)", example = "15")
        int dayOfMonth,

        @Min(value = 1, message = "durationMonths must be at least 1")
        @Schema(description = "Number of months the recurrence lasts", example = "6")
        int durationMonths
) {
    public CreateRecurringTransactionCommand toCommand(UUID userId) {
        return CreateRecurringTransactionCommand.builder()
                .userId(userId)
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
