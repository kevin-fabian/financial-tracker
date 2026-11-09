package com.fabiankevin.app.sync_devices.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Cursor for a household member's sync position")
public record SyncCursorQueryRequest(
        @NotNull(message = "Owner ID is required")
        @Schema(description = "Household member user ID", example = "member-uuid")
        UUID ownerId,

        @NotNull(message = "Last pulled sequence is required")
        @Schema(description = "The household member's last pulled sequence", example = "120")
        Long lastPulledSequence) {
}