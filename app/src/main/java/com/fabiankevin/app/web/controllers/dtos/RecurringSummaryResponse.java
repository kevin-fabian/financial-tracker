package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response containing the created recurring transaction summary")
public record RecurringSummaryResponse(
        @Schema(description = "Unique identifier of the recurring transaction", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Description", example = "Monthly subscription")
        String description,

        @Schema(description = "Amount", example = "15.99")
        double amount,

        @Schema(description = "Whether the amount varies each cycle", example = "false")
        boolean variableAmount,

        @Schema(description = "Category details")
        CategoryResponse category,

        @Schema(description = "Account details")
        AccountResponse account,

        @Schema(description = "Day of month", example = "15")
        int dayOfMonth,

        @Schema(description = "Next occurrence date")
        LocalDate nextOccurrenceDate,

        @Schema(description = "End date (null when noEndDate is true)")
        LocalDate endDate,

        @Schema(description = "Days remaining until next occurrence", example = "5")
        int remainingDays,

        @Schema(description = "Transaction status", example = "UPCOMING")
        TransactionStatus transactionStatus,

        @Schema(description = "Recurring transaction status", example = "ACTIVE")
        RecurringTransactionStatus status,

        @Schema(description = "User who updated the recurring transaction")
        UserResponse updatedBy,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static RecurringSummaryResponse from(RecurringTransactionSummary summary) {
        Category category = summary.category();
        Account account = summary.account();
        User updatedBy = summary.updatedBy();
        return RecurringSummaryResponse.builder()
                .id(summary.id())
                .description(summary.description())
                .amount(summary.amount())
                .variableAmount(summary.variableAmount())
                .category(category != null ? CategoryResponse.from(category) : null)
                .account(account != null ? AccountResponse.from(account, updatedBy) : null)
                .dayOfMonth(summary.dayOfMonth())
                .nextOccurrenceDate(summary.nextOccurrenceDate())
                .endDate(summary.endDate())
                .remainingDays(summary.remainingDays())
                .transactionStatus(summary.transactionStatus())
                .status(summary.status())
                .updatedBy(UserResponse.from(updatedBy))
                .createdAt(summary.createdAt())
                .updatedAt(summary.updatedAt())
                .build();
    }
}
