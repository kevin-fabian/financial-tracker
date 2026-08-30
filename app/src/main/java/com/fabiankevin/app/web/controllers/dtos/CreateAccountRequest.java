package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating an account")
public record CreateAccountRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must not exceed 128 characters")
        @Schema(description = "Name of the account", example = "GCASH")
        String name,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        @Schema(description = "Currency code (ISO 4217)", example = "PHP")
        String currency,

        @NotNull(message = "Type is required")
        @Schema(description = "Type of the account", example = "E_WALLET")
        AccountType type
) {
    public CreateAccountCommand toCommand(UUID userId) {
        return CreateAccountCommand.builder()
                .name(this.name())
                .currency(Currency.getInstance(this.currency()))
                .type(this.type)
                .userId(userId)
                .build();
    }
}
