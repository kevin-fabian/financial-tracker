package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record SharedSpace(
        UUID id,
        String spaceName, // "Family 2026 Budget", "Trip Expenses"
        UUID ownerUserId, // Primary owner (can have co-owners)
        List<SpaceParticipant> participants, // Core: Multiple participants with individual roles
        SharingMode sharingMode, // Global sharing mode for the space
        List<SharedResource> sharedResources, // Resources shared into this space
        SharingRule defaultSharingRule,   // Fallback for participants without custom rule
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt // Optional: time-limited
) {
    public SharedSpace {
        Optional.ofNullable(spaceName).orElseThrow(() -> new IllegalArgumentException("Space name is required"));
        Optional.ofNullable(ownerUserId).orElseThrow(() -> new IllegalArgumentException("Owner user ID is required"));
        Optional.ofNullable(sharingMode).orElseThrow(() -> new IllegalArgumentException("Sharing mode is required"));
        Optional.ofNullable(createdAt).orElseThrow(() -> new IllegalArgumentException("Created at is required"));
    }
}
