package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SyncChangeService {
    Map<UUID, List<SyncChange>> pullChanges(UUID requestingUserId, long lastSequenceId, List<SyncCursorQuery> cursors);
    SyncChange create(CreateSyncChangeCommand command);
}
