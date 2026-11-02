package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.persistence.entities.SyncStateEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultSyncStateRepository implements SyncStateRepository {
    private final JpaSyncStateRepository jpaSyncStateRepository;

    @Override
    public Optional<SyncState> findById(UUID id) {
        return jpaSyncStateRepository.findById(id)
                .map(SyncStateEntity::toModel);
    }

    @Override
    public SyncState save(SyncState syncState) {
        SyncStateEntity saved = jpaSyncStateRepository.save(SyncStateEntity.from(syncState));
        return saved.toModel();
    }

    @Override
    public List<SyncState> findAllByUserId(UUID userId) {
        return jpaSyncStateRepository.findAllByUserId(userId).stream()
                .map(SyncStateEntity::toModel)
                .toList();
    }

    @Override
    public long deleteAllByUserId(UUID userId) {
        return jpaSyncStateRepository.deleteAllByUserId(userId);
    }
}
