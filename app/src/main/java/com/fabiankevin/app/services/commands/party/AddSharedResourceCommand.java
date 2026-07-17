package com.fabiankevin.app.services.commands.party;

import com.fabiankevin.app.models.enums.party.ResourceType;
import lombok.Builder;

import java.util.List;

@Builder
public record AddSharedResourceCommand(
        ResourceType type,
        List<String> itemIds
) {
}
