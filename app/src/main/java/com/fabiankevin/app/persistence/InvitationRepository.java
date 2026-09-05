package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.household.Invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);

    void delete(UUID id);

    Optional<Invitation> findById(UUID id);

    Optional<Invitation> findPendingByHouseholdIdAndInviterAndInvitee(UUID householdId, UUID inviterUserId, UUID inviteeUserId);

    List<Invitation> findByInviterUserIdOrInviteeUserId(UUID userId);

    List<Invitation> findByInviteeUserId(UUID userId);
}
