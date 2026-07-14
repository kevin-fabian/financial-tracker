package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import lombok.Builder;

import java.util.List;

@Builder
public record AddSharedResourceCommand(
        ResourceType type,
        List<String> itemIds
) {
}
