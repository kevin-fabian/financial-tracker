package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing an account with aggregated summary data")
public record AccountSummaryResponse(
        @Schema(description = "Unique identifier of the account", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Name of the account", example = "GCASH")
        String name,
        @Schema(description = "Currency code of the account", example = "PHP")
        String currency,
        @Schema(description = "Type of the account", example = "E_WALLET")
        AccountType type,
        @Schema(description = "Total totalAmount for this account", example = "5000.00")
        double totalAmount,
        @Schema(description = "Percentage of total balance for this account", example = "35.5")
        double percentage,
        @Schema(description = "Total number of transactions for this account", example = "25")
        int totalTransactions
) {
    public static AccountSummaryResponse from(final AccountSummary accountSummary) {
        return AccountSummaryResponse.builder()
                .id(accountSummary.id())
                .name(accountSummary.name())
                .currency(accountSummary.currency().getCurrencyCode())
                .type(accountSummary.type())
                .totalAmount(accountSummary.totalAmount())
                .percentage(accountSummary.percentage())
                .totalTransactions(accountSummary.totalTransactions())
                .build();
    }
}
