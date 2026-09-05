package com.fabiankevin.app.web.controllers.dtos.household;

import com.fabiankevin.app.services.commands.household.PatchHouseholdCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a household. All fields are optional.")
public record PatchHouseholdRequest(
        @Schema(description = "Display name for the household", example = "Family 2026 Budget")
        String householdName) {
    public PatchHouseholdCommand toCommand(UUID id, UUID userId) {
        return PatchHouseholdCommand.builder()
                .id(id)
                .householdName(this.householdName())
                .playerId(userId)
                .build();
    }
}
