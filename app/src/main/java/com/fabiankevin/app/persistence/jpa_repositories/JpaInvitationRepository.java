package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.persistence.entities.InvitationEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    Optional<InvitationEntity> findByPartyIdAndInviterPlayerIdAndInviteePlayerIdAndStatus(UUID partyId, UUID inviterPlayerId, UUID inviteePlayerId, InvitationStatus status);

    List<InvitationEntity> findByInviterPlayerIdOrInviteePlayerId(UUID inviterPlayerId, UUID inviteePlayerId, Sort sort);

    @Query("select i from InvitationEntity i where i.inviteePlayerId = :userId and i.status = :status order by i.createdAt desc")
    List<InvitationEntity> findByInviteeUserId(UUID userId, InvitationStatus status, Sort sort);
}
