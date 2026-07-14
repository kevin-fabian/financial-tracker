package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SharingRule;
import com.fabiankevin.app.services.commands.shared_space.*;

import java.util.List;
import java.util.UUID;

public interface SharedSpaceService {
    SharedSpace createShare(CreateSharedSpaceCommand command);
    Invitation sendInvitation(SendInvitationCommand command);
    SharedSpace acceptInvitation(AcceptInvitationCommand command);
    Invitation rejectInvitation(RejectInvitationCommand command);
    Invitation revokeInvitation(RevokeInvitationCommand command);

    SharedSpace updateParticipantRule(UUID spaceId, UUID participantId, SharingRule rule);
    void removeParticipant(UUID spaceId, UUID participantId, UUID requesterId);

    List<SharedSpace> retrieveByUserId(UUID userId);
    List<Invitation> getInvitationsByUserId(UUID userId);
    List<SharedResource> getVisibleResources(UUID spaceId, UUID viewerId);
    SharedResource addResource(UUID spaceId, AddSharedResourceCommand command);

    List<UUID> getParticipantUserIds(UUID userId);
}