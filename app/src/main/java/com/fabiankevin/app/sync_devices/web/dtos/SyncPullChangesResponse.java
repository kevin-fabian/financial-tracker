package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for pulling sync changes")
public record SyncPullChangesResponse(
        @Valid
        @NotNull(message = "Scopes are required")
        @Schema(description = "Scoped list of changes per user")
        List<SyncScope> scopes) {

    public static SyncPullChangesResponse from(List<SyncCursorQuery> cursors, Map<UUID, List<SyncChange>> changesByUser) {
        List<SyncScope> scopes = new java.util.ArrayList<>();
        for (SyncCursorQuery cursor : cursors) {
            UUID ownerId = cursor.ownerId();
            List<SyncChange> changes = changesByUser.getOrDefault(ownerId, List.of());

            if (changes.isEmpty()) {
                scopes.add(SyncScope.empty(ownerId, cursor.lastPulledSequence()));
            } else {
                List<SyncChangeDto> changeDtos = changes.stream()
                        .map(SyncChangeDto::from)
                        .toList();

                Long nextCursor = changes.stream()
                        .mapToLong(SyncChange::sequenceId)
                        .max()
                        .orElse(cursor.lastPulledSequence());

                scopes.add(SyncScope.builder()
                        .userId(ownerId)
                        .changes(changeDtos)
                        .nextCursor(nextCursor)
                        .hasMore(false)
                        .build());
            }
        }
        return SyncPullChangesResponse.builder()
                .scopes(scopes)
                .build();
    }
}
