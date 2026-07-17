package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.PartySummary;
import com.fabiankevin.app.models.shared_space.SharedItem;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;

import java.util.List;
import java.util.UUID;

public interface PartyService {
    Party organize(OrganizePartyCommand command);

    void removeParticipant(UUID partyId, UUID participantId, UUID requesterId);

    List<PartySummary> retrieveByUserId(UUID userId);
    SharedItem addResource(UUID partyId, AddSharedResourceCommand command);

    List<UUID> getParticipantUserIds(UUID userId);

    void deleteParty(UUID partyId, UUID requesterId);

    Party patchParty(PatchPartyCommand command);
}
