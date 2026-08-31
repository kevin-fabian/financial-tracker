package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Optional;
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
        @Schema(description = "Whether the account is active", example = "true")
        boolean active,
        @Schema(description = "Total balance for this account", example = "5000.00")
        double totalBalance,
        @Schema(description = "Total number of transactions for this account", example = "25")
        int totalTransactions,
        @Schema(description = "User details associated with the account")
        UserResponse user) {
    public static AccountSummaryResponse from(final AccountSummary accountSummary) {
        Account account = accountSummary.account();
        return AccountSummaryResponse.builder()
                .id(account.id())
                .name(account.name())
                .currency(account.currency().getCurrencyCode())
                .type(account.type())
                .active(account.active())
                .totalBalance(accountSummary.totalBalance())
                .totalTransactions(accountSummary.totalTransactions())
                .user(Optional.ofNullable(accountSummary.user())
                        .map(UserResponse::from)
                        .orElse(null))
                .build();
    }
}
