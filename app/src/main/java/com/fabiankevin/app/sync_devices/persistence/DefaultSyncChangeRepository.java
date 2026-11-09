package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultSyncChangeRepository implements SyncChangeRepository {
    private final JpaSyncChangeRepository jpaSyncChangeRepository;

    @Override
    public SyncChange save(SyncChange syncChange) {
        SyncChangeEntity saved = jpaSyncChangeRepository.save(SyncChangeEntity.from(syncChange));
        return saved.toModel();
    }

    @Override
    public List<SyncChange> findChangesAfter(UUID userId, Long lastPulledSequence) {
        return jpaSyncChangeRepository.findByUserIdAndSequenceIdGreaterThanOrderBySequenceIdAsc(userId, lastPulledSequence).stream()
                .map(SyncChangeEntity::toModel)
                .toList();
    }
}
