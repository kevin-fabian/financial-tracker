package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<SyncChange> findChangesAfterBatch(UUID userId, Long lastSequenceId, int limit) {
        List<SyncChangeEntity> entities = jpaSyncChangeRepository.findByUserIdAndSequenceIdGreaterThanOrderBySequenceIdAsc(
                userId, lastSequenceId, PageRequest.of(0, limit));
        return entities.stream()
                .map(SyncChangeEntity::toModel)
                .toList();
    }

    @Override
    public Map<UUID, List<SyncChange>> findChangesAfterBatch(List<SyncCursorQuery> cursors) {
        if (cursors.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> cursorMap = cursors.stream()
                .collect(Collectors.toMap(SyncCursorQuery::ownerId, SyncCursorQuery::lastPulledSequence, Long::max));

        Set<UUID> userIds = new HashSet<>(cursorMap.keySet());

        List<SyncChangeEntity> entities = jpaSyncChangeRepository.findByUserIdIn(userIds);

        return entities.stream()
                .filter(entity -> entity.toModel().sequenceId() > cursorMap.getOrDefault(entity.toModel().userId(), 0L))
                .collect(Collectors.groupingBy(
                        entity -> entity.toModel().userId(),
                        Collectors.mapping(
                                SyncChangeEntity::toModel,
                                Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                        .sorted(Comparator.comparingLong(SyncChange::sequenceId))
                                        .toList())
                        )
                ));
    }

    @Override
    public Optional<Long> findMaxSequenceId(UUID userId) {
        return jpaSyncChangeRepository.findMaxSequenceIdByUserId(userId);
    }
}
