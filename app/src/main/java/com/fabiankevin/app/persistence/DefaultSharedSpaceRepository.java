package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.persistence.entities.SharedSpaceEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSharedSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultSharedSpaceRepository implements SharedSpaceRepository {
    private final JpaSharedSpaceRepository jpaSharedSpaceRepository;

    @Override
    public SharedSpace save(SharedSpace space) {
        SharedSpaceEntity saved = jpaSharedSpaceRepository.save(SharedSpaceEntity.from(space));
        return saved.toModel();
    }

    @Override
    public Optional<SharedSpace> findById(UUID id) {
        return jpaSharedSpaceRepository.findById(id).map(SharedSpaceEntity::toModel);
    }
}
