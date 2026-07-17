package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.persistence.entities.SharedSpaceEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSharedSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultSharedSpaceRepository implements SharedSpaceRepository {
    private final JpaSharedSpaceRepository jpaSharedSpaceRepository;

    @Override
    public Party save(Party space) {
        SharedSpaceEntity saved = jpaSharedSpaceRepository.save(SharedSpaceEntity.from(space));
        return saved.toModel();
    }

    @Override
    public Optional<Party> findById(UUID id) {
        return jpaSharedSpaceRepository.findById(id).map(SharedSpaceEntity::toModel);
    }

    @Override
    public Optional<Party> findByUserId(UUID userId) {
        return jpaSharedSpaceRepository.findByUserId(userId).stream()
                .findFirst()
                .map(SharedSpaceEntity::toModel);
    }

    @Override
    public List<Party> retrieveByUserId(UUID userId) {
        return jpaSharedSpaceRepository.findByUserId(userId).stream()
                .map(SharedSpaceEntity::toModel)
                .toList();
    }

    @Override
    public List<Party> findAllById(Iterable<UUID> ids) {
        return jpaSharedSpaceRepository.findAllById(ids).stream()
                .map(SharedSpaceEntity::toModel)
                .toList();
    }

    @Override
    public List<UUID> findParticipantUserIdsByUserId(UUID userId) {
        return jpaSharedSpaceRepository.findParticipantUserIdsByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaSharedSpaceRepository.deleteById(id);
    }
}
