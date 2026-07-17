package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SharedSpaceSummary;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;

import java.util.List;
import java.util.UUID;

public interface SharedSpaceService {
    SharedSpace createShare(CreateSharedSpaceCommand command);

    void removeParticipant(UUID spaceId, UUID participantId, UUID requesterId);

    List<SharedSpaceSummary> retrieveByUserId(UUID userId);
    SharedResource addResource(UUID spaceId, AddSharedResourceCommand command);

    List<UUID> getParticipantUserIds(UUID userId);
}
