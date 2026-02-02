package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.CreateAccountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating an account")
public record CreateAccountRequest(
        @NotBlank(message = "Name is required")
        @Schema(description = "Name of the account", example = "GCASH")
        String name,

        @NotBlank(message = "Currency is required")
        @Schema(description = "Currency code (ISO 4217)", example = "PHP")
        String currency
) {
    public CreateAccountCommand toCommand(UUID userId) {
        return CreateAccountCommand.builder()
                .name(this.name())
                .currency(Currency.getInstance(this.currency()))
                .userId(userId)
                .build();
    }
}
