package com.fabiankevin.app.sync_devices.domain.services.commands;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateSyncChangeCommand(
        UUID userId,
        ResourceType entityType,
        UUID entityId,
        SyncChangeOperationType operation,
        String payload) {
}
