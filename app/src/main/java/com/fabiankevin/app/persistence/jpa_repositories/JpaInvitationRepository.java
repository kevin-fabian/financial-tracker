package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.persistence.entities.InvitationEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    Optional<InvitationEntity> findByPartyIdAndInviterPlayerIdAndInviteePlayerIdAndStatus(UUID partyId, UUID inviterPlayerId, UUID inviteePlayerId, InvitationStatus status);

    List<InvitationEntity> findByInviterPlayerIdOrInviteePlayerId(UUID inviterPlayerId, UUID inviteePlayerId, Sort sort);
}
