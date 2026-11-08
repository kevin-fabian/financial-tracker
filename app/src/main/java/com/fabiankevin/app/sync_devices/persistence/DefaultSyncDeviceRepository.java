package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.domain.models.SyncDevice;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncDeviceRepository;
import com.fabiankevin.app.sync_devices.persistence.entities.SyncDeviceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultSyncDeviceRepository implements SyncDeviceRepository {
    private final JpaSyncStateRepository jpaSyncStateRepository;

    @Override
    public Optional<SyncDevice> findById(UUID id) {
        return jpaSyncStateRepository.findById(id)
                .map(SyncDeviceEntity::toModel);
    }

    @Override
    public Optional<SyncDevice> findByUserIdAndUserDevice(UUID userId, String userDevice) {
        return jpaSyncStateRepository.findByUserIdAndUserDevice(userId, userDevice)
                .map(SyncDeviceEntity::toModel);
    }

    @Override
    public SyncDevice save(SyncDevice syncDevice) {
        SyncDeviceEntity saved = jpaSyncStateRepository.save(SyncDeviceEntity.from(syncDevice));
        return saved.toModel();
    }

    @Override
    public List<SyncDevice> findAllByUserId(UUID userId) {
        return jpaSyncStateRepository.findAllByUserId(userId).stream()
                .map(SyncDeviceEntity::toModel)
                .toList();
    }

    @Override
    public long deleteAllByUserId(UUID userId) {
        return jpaSyncStateRepository.deleteAllByUserId(userId);
    }
}
