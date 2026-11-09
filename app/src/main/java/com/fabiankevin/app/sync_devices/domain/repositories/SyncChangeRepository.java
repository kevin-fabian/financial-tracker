package com.fabiankevin.app.sync_devices.domain.repositories;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;

public interface SyncChangeRepository {
    SyncChange save(SyncChange syncChange);
}
