package com.fabiankevin.app.web.controllers.dtos.household;

import com.fabiankevin.app.services.commands.household.OrganizeHouseholdCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a household")
public record OrganizeHouseholdRequest(
        @Schema(description = "Display name for the household", example = "Family 2026 Budget")
        @Size(max = 100, message = "Household name must not exceed 100 characters")
        String name) {
    public OrganizeHouseholdCommand toCommand(UUID partyLeaderId) {
        return OrganizeHouseholdCommand.builder()
                .leaderId(partyLeaderId)
                .householdName(name)
                .build();
    }
}
