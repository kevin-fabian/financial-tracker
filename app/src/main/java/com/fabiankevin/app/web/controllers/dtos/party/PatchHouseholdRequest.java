package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.services.commands.party.PatchHouseholdCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a shared space. All fields are optional.")
public record PatchHouseholdRequest(
        @Schema(description = "Display name for the space", example = "Family 2026 Budget")
        String partyName) {
    public PatchHouseholdCommand toCommand(UUID id, UUID userId) {
        return PatchHouseholdCommand.builder()
                .id(id)
                .partyName(this.partyName())
                .playerId(userId)
                .build();
    }
}
