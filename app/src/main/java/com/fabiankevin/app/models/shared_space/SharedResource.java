package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.ResourceType;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder
public record SharedResource(
        UUID id,
        ResourceType type,
        String resourceName,
        UUID ownerUserId,               // Who shared the resource
        List<String> itemIds,           // e.g., transaction IDs, budget IDs
        boolean sharedByOwner,
        Instant sharedAt
) {
    public SharedResource {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(itemIds, "itemIds");
        itemIds = Optional.ofNullable(itemIds).orElse(new ArrayList<>());
    }
}