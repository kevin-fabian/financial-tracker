package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a shared space. All fields are optional.")
public record PatchPartyRequest(
        @Schema(description = "Display name for the space", example = "Family 2026 Budget")
        String partyName,

        @Schema(description = "Global sharing mode for the space", example = "EVEN_SHARE")
        SharingMode sharingMode
) {
    public PatchPartyCommand toCommand(UUID id, UUID userId) {
        return PatchPartyCommand.builder()
                .id(id)
                .partyName(this.partyName())
                .sharingMode(this.sharingMode())
                .playerId(userId)
                .build();
    }
}
