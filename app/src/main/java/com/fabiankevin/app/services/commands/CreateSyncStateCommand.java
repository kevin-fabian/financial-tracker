package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateSyncStateCommand(
        UUID userId,
        String userDevice,
        BootstrapStatus status,
        BootstrapDirection direction) {
}
