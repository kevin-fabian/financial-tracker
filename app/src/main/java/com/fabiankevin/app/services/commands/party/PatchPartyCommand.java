package com.fabiankevin.app.services.commands.party;

import com.fabiankevin.app.models.enums.party.SharingMode;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchPartyCommand(
        UUID id,
        String partyName,
        SharingMode sharingMode,
        UUID playerId
) {
}
