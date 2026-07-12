package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        Instant updatedAt,
        Instant expiresAt // Optional: time-limited
) {
    public SharedSpace {
        Objects.requireNonNull(ownerUserId, "ownerUserId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(spaceName, "spaceName is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }
}
