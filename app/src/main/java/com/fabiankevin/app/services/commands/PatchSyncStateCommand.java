package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.sync_devices.domain.models.BootstrapStatus;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchSyncStateCommand(
        UUID id,
        UUID userId,
        BootstrapStatus syncStatus) {
}
