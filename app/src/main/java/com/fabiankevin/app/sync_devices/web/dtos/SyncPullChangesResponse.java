package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for pulling sync changes")
public record SyncPullChangesResponse(
        @Valid
        @NotNull(message = "Scopes are required")
        @Schema(description = "Scoped list of changes per user")
        List<SyncScopeDto> scopes) {

    public static SyncPullChangesResponse from(List<SyncCursorQuery> cursors, List<SyncScope> scopes) {
        List<SyncScopeDto> scopeDtos = new java.util.ArrayList<>();
        for (SyncCursorQuery cursor : cursors) {
            UUID ownerId = cursor.ownerId();
            SyncScope scope = scopes.stream()
                    .filter(s -> s.userId().equals(ownerId))
                    .findFirst()
                    .orElse(SyncScope.empty(ownerId, cursor.lastPulledSequence()));

            scopeDtos.add(SyncScopeDto.builder()
                    .userId(scope.userId())
                    .changes(scope.changes().stream().map(SyncChangeDto::from).toList())
                    .nextCursor(scope.nextCursor())
                    .hasMore(scope.hasMore())
                    .build());
        }
        return SyncPullChangesResponse.builder()
                .scopes(scopeDtos)
                .build();
    }
}
