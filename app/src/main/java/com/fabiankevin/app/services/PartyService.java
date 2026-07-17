package com.fabiankevin.app.services;

import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartySummary;
import com.fabiankevin.app.services.commands.party.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;

import java.util.List;
import java.util.UUID;

public interface PartyService {
    PartySummary organize(OrganizePartyCommand command);

    void kickPartyMember(UUID partyId, UUID participantId, UUID requesterId);

    List<PartySummary> retrieveByUserId(UUID userId);

    List<UUID> getPartyMembersUserId(UUID userId);

    void disbandParty(UUID partyId, UUID requesterId);

    Party patchParty(PatchPartyCommand command);
}
