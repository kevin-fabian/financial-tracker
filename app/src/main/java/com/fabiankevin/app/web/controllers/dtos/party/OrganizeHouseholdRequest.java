package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a household")
public record OrganizeHouseholdRequest(
        @Schema(description = "Display name for the household", example = "Family 2026 Budget")
        String name) {
    public OrganizeHouseholdCommand toCommand(UUID partyLeaderId) {
        return OrganizeHouseholdCommand.builder()
                .leaderId(partyLeaderId)
                .householdName(name)
                .build();
    }
}
