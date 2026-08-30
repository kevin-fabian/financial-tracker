package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.PatchAccountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching an account. All fields are optional.")
public record PatchAccountRequest(
        @Size(max = 128, message = "Name must not exceed 128 characters")
        @Schema(description = "Name of the account", example = "GCASH")
        String name,
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        @Schema(description = "Currency code of the account", example = "PHP")
        String currency,
        @Schema(description = "Type of the account", example = "E_WALLET")
        com.fabiankevin.app.models.enums.AccountType type
) {
    public PatchAccountCommand toCommand(UUID id, UUID userId) {
        return PatchAccountCommand.builder()
                .id(id)
                .name(this.name())
                .currency(this.currency() != null ? Currency.getInstance(this.currency()) : null)
                .type(this.type)
                .userId(userId)
                .build();
    }
}

