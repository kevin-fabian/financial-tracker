package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.SyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSyncStateRepository extends JpaRepository<SyncStateEntity, UUID> {
    List<SyncStateEntity> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
