package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSyncChangeRepository extends JpaRepository<SyncChangeEntity, UUID> {
}
