package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder(toBuilder = true)
@Schema(description = "A scope of sync changes for a single user")
public record SyncScopeDto(
        @NotNull(message = "User ID is required")
        @Schema(description = "User identifier this scope belongs to", example = "user-a-uuid")
        UUID userId,

        @Valid
        @Schema(description = "List of changes for this user")
        List<SyncChangeDto> changes,

        @Schema(description = "Next cursor value for pagination", example = "121")
        Long nextCursor,

        @Schema(description = "Whether more changes are available", example = "false")
        boolean hasMore) {

    public static SyncScopeDto empty(UUID userId, Long nextCursor) {
        return builder()
                .userId(userId)
                .changes(List.of())
                .nextCursor(nextCursor)
                .hasMore(false)
                .build();
    }

    public static SyncScopeDto from(SyncScope syncScope) {
        return SyncScopeDto.builder()
                .userId(syncScope.userId())
                .changes(syncScope.changes().stream()
                        .map(SyncChangeDto::from)
                        .collect(Collectors.toList()))
                .nextCursor(syncScope.nextCursor())
                .hasMore(syncScope.hasMore())
                .build();
    }
}
