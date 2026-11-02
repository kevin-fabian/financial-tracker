package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.models.enums.SyncStatus;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchSyncStateCommand(
        UUID id,
        UUID userId,
        SyncStatus syncStatus) {
}
