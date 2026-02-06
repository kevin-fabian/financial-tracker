package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.PatchAccountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching an account. All fields are optional.")
public record PatchAccountRequest(
        @Schema(description = "Name of the account", example = "GCASH")
        String name,
        @Schema(description = "Currency code of the account", example = "PHP")
        String currency
) {
    public PatchAccountCommand toCommand(UUID id, UUID userId) {
        return PatchAccountCommand.builder()
                .id(id)
                .name(this.name())
                .currency(this.currency() != null ? Currency.getInstance(this.currency()) : null)
                .userId(userId)
                .build();
    }
}

