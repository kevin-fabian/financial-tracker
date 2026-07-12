package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.SharingMode;

import java.util.UUID;

public record CreateSharedSpaceCommand(
        UUID ownerUserId,
        String spaceName,
        SharingMode sharingMode
) {
}
