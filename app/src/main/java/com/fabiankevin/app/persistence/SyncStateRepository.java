package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SyncState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncStateRepository {
    Optional<SyncState> findById(UUID id);

    SyncState save(SyncState syncState);

    List<SyncState> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
