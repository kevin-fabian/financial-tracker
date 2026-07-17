package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchPartyCommand(
        UUID id,
        String partyName,
        SharingMode sharingMode,
        UUID userId
) {
}
