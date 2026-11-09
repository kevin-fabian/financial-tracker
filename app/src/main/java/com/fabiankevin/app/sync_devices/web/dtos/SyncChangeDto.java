package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "A single sync change returned in a scope")
public record SyncChangeDto(
        @NotNull(message = "Sequence ID is required")
        @Schema(description = "Monotonic sequence identifier", example = "121")
        Long sequenceId,

        @NotNull(message = "Entity type is required")
        @Schema(description = "Type of the affected entity", example = "TRANSACTIONS")
        ResourceType entityType,

        @NotNull(message = "Entity ID is required")
        @Schema(description = "Unique identifier of the affected entity", example = "transaction-uuid")
        UUID entityId,

        @NotNull(message = "Operation is required")
        @Schema(description = "Type of operation performed", example = "CREATE")
        SyncChangeOperationType operation) {

    public static SyncChangeDto from(SyncChange syncChange) {
        return SyncChangeDto.builder()
                .sequenceId(syncChange.sequenceId())
                .entityType(syncChange.entityType())
                .entityId(syncChange.entityId())
                .operation(syncChange.operation())
                .build();
    }
}
