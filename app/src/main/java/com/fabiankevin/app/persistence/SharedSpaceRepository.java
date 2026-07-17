package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.SharedSpace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedSpaceRepository {
    SharedSpace save(SharedSpace space);

    Optional<SharedSpace> findById(UUID id);

    Optional<SharedSpace> findByUserId(UUID userId);

    List<SharedSpace> retrieveByUserId(UUID userId);

    List<SharedSpace> findAllById(Iterable<UUID> ids);

    List<UUID> findParticipantUserIdsByUserId(UUID userId);
}
