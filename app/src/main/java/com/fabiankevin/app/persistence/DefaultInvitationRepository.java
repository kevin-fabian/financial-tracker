package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.persistence.entities.InvitationEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultInvitationRepository implements InvitationRepository {
    private final JpaInvitationRepository jpaInvitationRepository;

    @Override
    public Invitation save(Invitation invitation) {
        InvitationEntity saved = jpaInvitationRepository.save(InvitationEntity.from(invitation));
        return saved.toModel();
    }

    @Override
    public Optional<Invitation> findById(UUID id) {
        return jpaInvitationRepository.findById(id).map(InvitationEntity::toModel);
    }
}
