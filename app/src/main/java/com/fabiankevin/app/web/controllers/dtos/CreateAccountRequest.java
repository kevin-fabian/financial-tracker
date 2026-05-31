package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.IconData;
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
        String currency,

        @Schema(description = "Type of the account", example = "E_WALLET")
        com.fabiankevin.app.models.enums.AccountType type,

        @Schema(description = "Icon for the account")
        CreateIconRequest icon
) {
    public CreateAccountCommand toCommand(UUID userId) {
        IconData iconData = this.icon != null ? this.icon.toIconData() : null;
        return CreateAccountCommand.builder()
                .name(this.name())
                .currency(Currency.getInstance(this.currency()))
                .type(this.type)
                .icon(iconData)
                .userId(userId)
                .build();
    }
}
