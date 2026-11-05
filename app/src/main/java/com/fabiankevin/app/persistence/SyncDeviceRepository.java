package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SyncDevice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncDeviceRepository {
    Optional<SyncDevice> findById(UUID id);

    Optional<SyncDevice> findByUserIdAndUserDevice(UUID userId, String userDevice);

    SyncDevice save(SyncDevice syncDevice);

    List<SyncDevice> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
