package com.fabiankevin.app.sync_devices.domain.repositories;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;

import java.util.Optional;
import java.util.UUID;

public interface SyncChangeRepository {
    SyncChange save(SyncChange syncChange);

    Page<SyncChange> findChangesAfterBatch(UUID userId, Long lastSequenceId, PageQuery pageQuery);

    Optional<Long> findMaxSequenceId(UUID userId);
}
