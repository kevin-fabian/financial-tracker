package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.BootstrapStatus;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a sync state")
public record PatchSyncDeviceRequest(
        @NotNull(message = "Status is required")
        @Schema(description = "New bootstrap status", example = "COMPLETED")
        BootstrapStatus status) {

    public PatchSyncStateCommand toCommand(UUID id, UUID userId) {
        return PatchSyncStateCommand.builder()
                .id(id)
                .userId(userId)
                .syncStatus(this.status)
                .build();
    }
}
