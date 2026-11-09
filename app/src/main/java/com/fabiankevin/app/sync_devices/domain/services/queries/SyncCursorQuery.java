package com.fabiankevin.app.sync_devices.domain.services.queries;

import com.fabiankevin.app.sync_devices.web.dtos.SyncCursorQueryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Cursor for a single user's sync position")
public record SyncCursorQuery(
        @NotNull(message = "Owner ID is required")
        @Schema(description = "User/owner identifier", example = "user-a-uuid")
        UUID ownerId,

        @NotNull(message = "Last pulled sequence is required")
        @Min(value = 0, message = "Last pulled sequence must be non-negative")
        @Schema(description = "Last sequence ID pulled by the client", example = "120")
        Long lastPulledSequence) {

    public static SyncCursorQuery from(SyncCursorQueryRequest request) {
        return SyncCursorQuery.builder()
                .ownerId(request.ownerId())
                .lastPulledSequence(request.lastPulledSequence())
                .build();
    }
}
