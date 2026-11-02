package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;

import java.util.List;
import java.util.UUID;

public interface SyncStateService {
    SyncState createSyncState(CreateSyncStateCommand command);

    SyncState patchSyncState(PatchSyncStateCommand command);

    SyncState getSyncStateById(UUID id, UUID userId);

    List<SyncState> getAllSyncStatesByUserId(UUID userId);
}
