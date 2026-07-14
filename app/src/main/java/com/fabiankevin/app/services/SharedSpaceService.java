package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.services.commands.shared_space.*;

import java.util.List;
import java.util.UUID;

public interface SharedSpaceService {
    SharedSpace createShare(CreateSharedSpaceCommand command);
    Invitation sendInvitation(SendInvitationCommand command);
    SharedSpace acceptInvitation(AcceptInvitationCommand command);
    Invitation rejectInvitation(RejectInvitationCommand command);

    void removeParticipant(UUID spaceId, UUID participantId, UUID requesterId);

    List<SharedSpace> retrieveByUserId(UUID userId);
    List<Invitation> getInvitationsByUserId(UUID userId);
    SharedResource addResource(UUID spaceId, AddSharedResourceCommand command);

    List<UUID> getParticipantUserIds(UUID userId);
}