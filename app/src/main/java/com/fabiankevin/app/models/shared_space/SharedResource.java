package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder
public record SharedResource(
        UUID id,
        ResourceType type,
        List<String> items,           // e.g., transaction IDs, budget IDs
        Instant sharedAt
) {
    public SharedResource {
        Objects.requireNonNull(type, "type");
        items = Optional.ofNullable(items).orElse(new ArrayList<>());
    }
}