package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.persistence.entities.SyncDeviceEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSyncStateRepository;
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
