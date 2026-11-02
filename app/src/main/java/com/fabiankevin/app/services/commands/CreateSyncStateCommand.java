package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateSyncStateCommand(
        UUID userId,
        String userDevice,
        ResourceType resourceType,
        SyncStatus syncStatus) {
}
