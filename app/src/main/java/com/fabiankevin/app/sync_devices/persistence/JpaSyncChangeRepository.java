package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface JpaSyncChangeRepository extends JpaRepository<SyncChangeEntity, UUID> {
    List<SyncChangeEntity> findByUserIdIn(Set<UUID> userIds);

    @Query("SELECT MAX(e.sequenceId) FROM SyncChangeEntity e WHERE e.userId = :userId")
    Optional<Long> findMaxSequenceIdByUserId(UUID userId);
}
