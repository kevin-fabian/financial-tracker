package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AddSharedResourceCommand(
        ResourceType type,
        UUID ownerUserId,
        List<String> itemIds,
        boolean sharedByOwner
) {
}
