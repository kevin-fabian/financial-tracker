package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record CreateSharedSpaceCommand(
        UUID ownerUserId,
        String spaceName,
        SharingMode sharingMode
) {
    public CreateSharedSpaceCommand {
        Objects.requireNonNull(ownerUserId, "Owner user ID cannot be null");
        Objects.requireNonNull(sharingMode, "Sharing mode cannot be null");
    }
}
