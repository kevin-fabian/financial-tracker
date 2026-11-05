package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating an initial sync state")
public record CreateSyncDeviceRequest(
        @NotNull(message = "Bootstrap status is required")
        @Schema(description = "Bootstrap status", example = "IN_PROGRESS")
        BootstrapStatus status,
        @NotNull(message = "Bootstrap direction is required")
        @Schema(description = "Bootstrap direction", example = "PULL")
        BootstrapDirection direction) {

    public CreateSyncStateCommand toCommand(UUID userId, String userDeviceId) {
        return CreateSyncStateCommand.builder()
                .userId(userId)
                .userDeviceId(userDeviceId)
                .status(this.status)
                .direction(this.direction)
                .build();
    }
}
