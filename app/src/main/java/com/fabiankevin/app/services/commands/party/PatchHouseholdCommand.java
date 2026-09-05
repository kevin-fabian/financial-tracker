package com.fabiankevin.app.services.commands.party;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchHouseholdCommand(
        UUID id,
        String householdName,
        UUID playerId
) {
}
