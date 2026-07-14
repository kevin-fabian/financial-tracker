package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.Invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);

    Optional<Invitation> findById(UUID id);

    Optional<Invitation> findPendingByInviterAndInvitee(UUID inviterUserId, UUID inviteeUserId);

    List<Invitation> findByInviterUserIdOrInviteeUserId(UUID userId);
}
