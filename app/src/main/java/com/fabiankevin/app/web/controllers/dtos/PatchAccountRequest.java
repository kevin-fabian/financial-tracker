package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.IconData;
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
        String currency,
        @Schema(description = "Type of the account", example = "E_WALLET")
        com.fabiankevin.app.models.enums.AccountType type,
        @Schema(description = "Icon for the account")
        IconResponse icon
) {
    public PatchAccountCommand toCommand(UUID id, UUID userId) {
        IconData iconData = this.icon != null ? toIconData(this.icon) : null;
        return PatchAccountCommand.builder()
                .id(id)
                .name(this.name())
                .currency(this.currency() != null ? Currency.getInstance(this.currency()) : null)
                .type(this.type)
                .icon(iconData)
                .userId(userId)
                .build();
    }

    private IconData toIconData(IconResponse icon) {
        return IconData.builder()
                .id(icon.id())
                .codePoint(icon.codePoint())
                .fontFamily(icon.fontFamily())
                .build();
    }
}

