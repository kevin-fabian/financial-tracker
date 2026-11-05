package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for a sync state")
public record SyncDeviceResponse(
        @Schema(description = "Unique identifier of the sync state")
        UUID id,
        @Schema(description = "Identifier of the user's device", example = "iphone-15")
        String userDevice,
        @Schema(description = "Bootstrap status")
        BootstrapStatus status,
        @Schema(description = "Bootstrap direction")
        BootstrapDirection direction,
        @Schema(description = "Timestamp when the sync state was created")
        Instant createdAt,
        @Schema(description = "Timestamp when the sync state was last updated")
        Instant updatedAt) {

    public static SyncDeviceResponse from(SyncDevice syncDevice) {
        return SyncDeviceResponse.builder()
                .id(syncDevice.id())
                .userDevice(syncDevice.userDevice())
                .status(syncDevice.status())
                .direction(syncDevice.direction())
                .createdAt(syncDevice.createdAt())
                .updatedAt(syncDevice.updatedAt())
                .build();
    }
}
