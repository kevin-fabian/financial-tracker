package com.fabiankevin.app.sync_devices.domain.repositories;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;

import java.util.List;
import java.util.UUID;

public interface SyncChangeRepository {
    SyncChange save(SyncChange syncChange);

    List<SyncChange> findChangesAfter(UUID userId, Long lastPulledSequence);
}
