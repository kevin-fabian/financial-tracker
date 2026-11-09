package com.fabiankevin.app.sync_devices.domain.models;

import com.fabiankevin.app.models.enums.ResourceType;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record SyncChange(
        UUID id,
        UUID userId,
        String userDevice,
        Long sequenceId,
        ResourceType entityType,
        UUID entityId,
        SyncChangeOperationType operation,
        String payload,
        Instant createdAt) {

    public SyncChange {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(sequenceId, "Sequence ID is required");
        Objects.requireNonNull(entityType, "Entity type is required");
        Objects.requireNonNull(entityId, "Entity ID is required");
        Objects.requireNonNull(operation, "Operation is required");
        Objects.requireNonNull(payload, "Payload is required");
        Objects.requireNonNull(createdAt, "Created at timestamp is required");
    }
}
