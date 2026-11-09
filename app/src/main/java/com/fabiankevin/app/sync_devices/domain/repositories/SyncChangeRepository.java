package com.fabiankevin.app.sync_devices.domain.repositories;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SyncChangeRepository {
    SyncChange save(SyncChange syncChange);

    Map<UUID, List<SyncChange>> findChangesAfterBatch(List<SyncCursorQuery> cursors);
}
