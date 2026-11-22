package com.fabiankevin.app.sync_devices.domain.services.commands;

import com.fabiankevin.app.models.enums.EntityType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateSyncChangeCommand(
        UUID userId,
        EntityType entityType,
        UUID entityId,
        SyncChangeOperationType operation,
        String payload) {
}
