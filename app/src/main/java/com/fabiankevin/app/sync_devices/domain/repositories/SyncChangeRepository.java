package com.fabiankevin.app.sync_devices.domain.repositories;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SyncChangeRepository {
    SyncChange save(SyncChange syncChange);

    List<SyncChange> findChangesAfterBatch(UUID userId, Long lastSequenceId, int limit);

    Map<UUID, List<SyncChange>> findChangesAfterBatch(List<SyncCursorQuery> cursors);

    Optional<Long> findMaxSequenceId(UUID userId);
}
