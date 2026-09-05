package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing an account record")
public record AccountResponse(
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
        @Schema(description = "Timestamp when the account was created", example = "2025-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "Timestamp when the account was last updated", example = "2025-06-15T10:30:00Z")
        Instant updatedAt,
        @Schema(description = "User who owns the account",
                exampleClasses = UserResponse.class)
        UserResponse user) {
    public static AccountResponse from(final Account account) {
        return AccountResponse.builder()
                .id(account.id())
                .name(account.name())
                .currency(account.currency().getCurrencyCode())
                .type(account.type())
                .active(account.active())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .user(UserResponse.from(account.user()))
                .build();
    }

    public static AccountResponse from(final Account account, final User user) {
        return AccountResponse.builder()
                .id(account.id())
                .name(account.name())
                .currency(account.currency().getCurrencyCode())
                .type(account.type())
                .active(account.active())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .user(UserResponse.from(user))
                .build();
    }
}
