package com.fabiankevin.app.sync_devices.domain.models;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record SyncScope(
        UUID userId,
        List<SyncChange> changes,
        Long nextCursor,
        boolean hasMore) {

    public static SyncScope empty(UUID userId, Long nextCursor) {
        return builder()
                .userId(userId)
                .changes(List.of())
                .nextCursor(nextCursor)
                .hasMore(false)
                .build();
    }
}
