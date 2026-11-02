package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record SyncState(
        UUID id,
        UUID userId,
        String userDevice,
        ResourceType resourceType,
        SyncStatus syncStatus,
        Instant createdAt,
        Instant updatedAt) {

    public SyncState {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(resourceType, "Resource type is required");
        Objects.requireNonNull(syncStatus, "Sync status is required");
    }
}
