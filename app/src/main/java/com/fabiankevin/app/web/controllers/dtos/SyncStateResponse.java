package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for a sync state")
public record SyncStateResponse(
        @Schema(description = "Unique identifier of the sync state")
        UUID id,
        @Schema(description = "Identifier of the user's device")
        String userDevice,
        @Schema(description = "Type of resource being synced")
        ResourceType resourceType,
        @Schema(description = "Current sync status")
        SyncStatus syncStatus,
        @Schema(description = "Timestamp when the sync state was created")
        Instant createdAt,
        @Schema(description = "Timestamp when the sync state was last updated")
        Instant updatedAt) {

    public static SyncStateResponse from(SyncState syncState) {
        return SyncStateResponse.builder()
                .id(syncState.id())
                .userDevice(syncState.userDevice())
                .resourceType(syncState.resourceType())
                .syncStatus(syncState.syncStatus())
                .createdAt(syncState.createdAt())
                .updatedAt(syncState.updatedAt())
                .build();
    }
}
