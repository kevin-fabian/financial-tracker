package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpaceSummary;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;

import java.util.List;
import java.util.UUID;

public interface PartyService {
    Party organize(OrganizePartyCommand command);

    void removeParticipant(UUID spaceId, UUID participantId, UUID requesterId);

    List<SharedSpaceSummary> retrieveByUserId(UUID userId);
    SharedResource addResource(UUID spaceId, AddSharedResourceCommand command);

    List<UUID> getParticipantUserIds(UUID userId);

    void deleteSharedSpace(UUID spaceId, UUID requesterId);

    Party patchSharedSpace(PatchPartyCommand command);
}
