package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.Invitation;
import com.fabiankevin.app.persistence.entities.InvitationEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public void delete(UUID id) {
        jpaInvitationRepository.deleteById(id);
    }

    @Override
    public Optional<Invitation> findById(UUID id) {
        return jpaInvitationRepository.findById(id).map(InvitationEntity::toModel);
    }

    @Override
    public Optional<Invitation> findPendingByPartyIdAndInviterAndInvitee(UUID partyId, UUID inviterUserId, UUID inviteeUserId) {
        return jpaInvitationRepository.findByPartyIdAndInviterPlayerIdAndInviteePlayerIdAndStatus(partyId, inviterUserId, inviteeUserId, InvitationStatus.PENDING)
                .map(InvitationEntity::toModel);
    }

    @Override
    public List<Invitation> findByInviterUserIdOrInviteeUserId(UUID userId) {
        return jpaInvitationRepository.findByInviterPlayerIdOrInviteePlayerId(userId, userId, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(InvitationEntity::toModel)
                .toList();
    }

    @Override
    public List<Invitation> findByInviteeUserId(UUID userId) {
        return jpaInvitationRepository.findByInviteeUserId(userId, InvitationStatus.PENDING, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(InvitationEntity::toModel)
                .toList();
    }
}
