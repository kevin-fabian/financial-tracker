package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSyncChangeRepository extends JpaRepository<SyncChangeEntity, UUID> {
    List<SyncChangeEntity> findByUserIdAndSequenceIdGreaterThanOrderBySequenceIdAsc(UUID userId, Long lastPulledSequence);
}
