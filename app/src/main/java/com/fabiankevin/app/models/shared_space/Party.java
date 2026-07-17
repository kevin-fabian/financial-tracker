package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
public record Party(
        UUID id,
        String name, // "Family 2026 Budget", "Trip Expenses"
        UUID partyLeaderId, // Primary owner (can have co-owners)
        List<Player> participants, // Core: Multiple participants with individual roles
        SharingMode sharingMode, // Global sharing mode for the space
        List<SharedResource> sharedResources, // Resources shared into this space
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public Party {
        Objects.requireNonNull(partyLeaderId, "partyLeaderId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        sharedResources = Optional.ofNullable(sharedResources).orElse(new ArrayList<>());
        participants = Optional.ofNullable(participants).orElse(new ArrayList<>());
    }
}
