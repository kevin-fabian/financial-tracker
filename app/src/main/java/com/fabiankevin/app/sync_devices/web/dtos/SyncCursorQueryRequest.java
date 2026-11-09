package com.fabiankevin.app.sync_devices.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Cursor for a single user's sync position")
public record SyncCursorQueryRequest(
        @NotNull(message = "Owner ID is required")
        @Schema(description = "User/owner identifier", example = "user-a-uuid")
        UUID ownerId,

        @NotNull(message = "Last pulled sequence is required")
        @Min(value = 0, message = "Last pulled sequence must be non-negative")
        @Schema(description = "Last sequence ID pulled by the client", example = "120")
        Long lastPulledSequence) {
}