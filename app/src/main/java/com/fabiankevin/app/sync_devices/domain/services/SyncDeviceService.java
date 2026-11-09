package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncDevice;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SyncDeviceService {
    void prepare(UUID userId);

    SyncDevice create(CreateSyncStateCommand command);

    SyncDevice patch(PatchSyncStateCommand command);

    SyncDevice getById(UUID id, UUID userId);

    List<SyncDevice> getAllByUserId(UUID userId);

    Map<UUID, List<SyncChange>> pullChanges(List<SyncCursorQuery> cursors);
}
