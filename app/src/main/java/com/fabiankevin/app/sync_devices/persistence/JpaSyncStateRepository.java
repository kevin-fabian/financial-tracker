package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.persistence.entities.SyncDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSyncStateRepository extends JpaRepository<SyncDeviceEntity, UUID> {
    List<SyncDeviceEntity> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);

    Optional<SyncDeviceEntity> findByUserIdAndUserDevice(UUID userId, String userDevice);
}
