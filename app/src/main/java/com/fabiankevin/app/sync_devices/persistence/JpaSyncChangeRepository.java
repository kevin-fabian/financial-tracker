package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface JpaSyncChangeRepository extends JpaRepository<SyncChangeEntity, UUID> {
    List<SyncChangeEntity> findByUserIdIn(Set<UUID> userIds);

    @Query("SELECT MAX(e.sequenceId) FROM SyncChangeEntity e WHERE e.userId = :userId")
    Optional<Long> findMaxSequenceIdByUserId(UUID userId);

    @Query("SELECT e FROM SyncChangeEntity e WHERE e.userId = :userId AND e.sequenceId > :lastSequenceId ORDER BY e.sequenceId ASC")
    Page<SyncChangeEntity> findByUserIdAndSequenceIdGreaterThanOrderBySequenceIdAscPaged(
            @Param("userId") UUID userId,
            @Param("lastSequenceId") Long lastSequenceId,
            Pageable pageable);
}
