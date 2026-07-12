package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.ResourceType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        itemIds = List.copyOf(itemIds);
    }
}