package com.fabiankevin.app.services.commands.party;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchPartyCommand(
        UUID id,
        String partyName,
        UUID playerId
) {
}
