package com.fabiankevin.app.accounts.web.dtos;

import com.fabiankevin.app.accounts.domain.models.AccountType;
import com.fabiankevin.app.accounts.domain.services.commands.CreateAccountCommand;
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
        @Schema(description = "Client-provided UUID for the account. If not provided, a server-generated UUID will be used", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,
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
                .id(this.id)
                .name(this.name())
                .currency(Currency.getInstance(this.currency()))
                .type(this.type)
                .userId(userId)
                .build();
    }
}
