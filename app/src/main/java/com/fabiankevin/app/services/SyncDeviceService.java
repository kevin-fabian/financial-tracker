package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;

import java.util.List;
import java.util.UUID;

public interface SyncDeviceService {
    void prepare(UUID userId);

    SyncDevice create(CreateSyncStateCommand command);

    SyncDevice patch(PatchSyncStateCommand command);

    SyncDevice getById(UUID id, UUID userId);

    List<SyncDevice> getAllByUserId(UUID userId);
}
