package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Account;
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
        @Schema(description = "Timestamp when the account was created")
        Instant createdAt,
        @Schema(description = "Timestamp when the account was last updated")
        Instant updatedAt
) {
    public static AccountResponse from(final Account account) {
        return AccountResponse.builder()
                .id(account.id())
                .name(account.name())
                .currency(account.currency().getCurrencyCode())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .build();
    }
}
