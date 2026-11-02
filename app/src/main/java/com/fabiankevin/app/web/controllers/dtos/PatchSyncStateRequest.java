package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a sync state")
public record PatchSyncStateRequest(
        @NotNull(message = "Sync status is required")
        @Schema(description = "New sync status", example = "COMPLETED")
        SyncStatus syncStatus) {

    public PatchSyncStateCommand toCommand(UUID id, UUID userId) {
        return PatchSyncStateCommand.builder()
                .id(id)
                .userId(userId)
                .syncStatus(this.syncStatus)
                .build();
    }
}
