package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateSharedSpaceCommand(
        UUID ownerUserId,
        String spaceName,
        SharingMode sharingMode,
        List<AddSharedResourceCommand> resources
) {
}
