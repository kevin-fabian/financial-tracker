package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a party")
public record OrganizePartyRequest(
    @Schema(description = "Display name for the party", example = "Family 2026 Budget")
    String name,

    @NotNull(message = "Sharing mode is required")
    @Schema(description = "Global sharing mode for the party", example = "MUTUAL_SHARING")
    SharingMode sharingMode
) {
    public OrganizePartyCommand toCommand(UUID partyLeaderId) {
        return OrganizePartyCommand.builder()
            .partyLeaderId(partyLeaderId)
            .partyName(name)
            .sharingMode(sharingMode)
            .build();
    }
}
