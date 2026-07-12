package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
public record SharedSpace(
        UUID id,
        String spaceName, // "Family 2026 Budget", "Trip Expenses"
        UUID ownerUserId, // Primary owner (can have co-owners)
        List<SpaceParticipant> participants, // Core: Multiple participants with individual roles
        SharingMode sharingMode, // Global sharing mode for the space
        List<SharedResource> sharedResources, // Resources shared into this space
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public SharedSpace {
        Objects.requireNonNull(ownerUserId, "ownerUserId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(spaceName, "spaceName is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        sharedResources = Optional.ofNullable(sharedResources).orElse(new ArrayList<>());
        participants = Optional.ofNullable(participants).orElse(new ArrayList<>());
    }
}
