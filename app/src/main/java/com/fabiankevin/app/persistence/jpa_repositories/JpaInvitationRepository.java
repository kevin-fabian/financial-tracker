package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.persistence.entities.InvitationEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    Optional<InvitationEntity> findByHouseholdIdAndInviterUserIdAndInviteeUserIdAndStatus(UUID householdId, UUID inviterUserId, UUID inviteeUserId, InvitationStatus status);

    List<InvitationEntity> findByInviterUserIdOrInviteeUserId(UUID inviterUserId, UUID inviteeUserId, Sort sort);

    @Query("select i from InvitationEntity i where i.inviteeUserId = :userId and i.status = :status order by i.createdAt desc")
    List<InvitationEntity> findByInviteeUserId(UUID userId, InvitationStatus status, Sort sort);
}
