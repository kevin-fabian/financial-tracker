package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.models.enums.EntityType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "A single sync change returned in a scope")
public record SyncChangeDto(
        @NotNull(message = "Sequence ID is required")
        @Schema(description = "Monotonic sequence identifier", example = "121")
        Long sequenceId,

        @NotNull(message = "Entity type is required")
        @Schema(description = "Type of the affected entity", example = "TRANSACTIONS")
        EntityType entityType,

        @NotNull(message = "Entity ID is required")
        @Schema(description = "Unique identifier of the affected entity", example = "transaction-uuid")
        UUID entityId,

        @NotNull(message = "Operation is required")
        @Schema(description = "Type of operation performed", example = "CREATE")
        SyncChangeOperationType operation,

        @Schema(description = "JSON payload containing entity data", example = "{\"name\":\"Test\"}")
        Map<String, Object> payload) {

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    public static SyncChangeDto from(SyncChange syncChange) {
        try {
            Map<String, Object> payloadMap = jsonMapper.readValue(
                    syncChange.payload(),
                    new TypeReference<>() {
                    });
            return SyncChangeDto.builder()
                    .sequenceId(syncChange.sequenceId())
                    .entityType(syncChange.entityType())
                    .entityId(syncChange.entityId())
                    .operation(syncChange.operation())
                    .payload(payloadMap)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payload JSON", e);
        }
    }
}
