package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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
    public Page<SyncChange> findChangesAfterBatch(UUID userId, Long lastSequenceId, PageQuery pageQuery) {
        var pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.ASC, "sequenceId")
        );

        var page = jpaSyncChangeRepository.findByUserIdAndSequenceIdGreaterThanOrderBySequenceIdAscPaged(
                userId, lastSequenceId, pageable);

        return new Page<>(
                page.getContent().stream()
                        .map(SyncChangeEntity::toModel)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    @Override
    public Optional<Long> findMaxSequenceId(UUID userId) {
        return jpaSyncChangeRepository.findMaxSequenceIdByUserId(userId);
    }
}
