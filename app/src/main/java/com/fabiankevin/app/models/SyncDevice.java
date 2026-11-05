package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record SyncDevice(
        UUID id,
        UUID userId,
        UUID deviceId,
        UUID userDevice,
        BootstrapStatus status,
        BootstrapDirection direction,
        Instant lastSyncedAt,
        Instant createdAt,
        Instant updatedAt) {

    public SyncDevice {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(status, "Bootstrap status is required");
        Objects.requireNonNull(direction, "Bootstrap direction is required");
    }
}
