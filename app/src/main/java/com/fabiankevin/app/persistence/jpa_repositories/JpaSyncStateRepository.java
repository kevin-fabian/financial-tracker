package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.SyncDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSyncStateRepository extends JpaRepository<SyncDeviceEntity, UUID> {
    List<SyncDeviceEntity> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
