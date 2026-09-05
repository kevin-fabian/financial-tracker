package com.fabiankevin.app.services;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;

import java.util.List;
import java.util.UUID;

public interface HouseholdService {
    HouseholdSummary organize(OrganizeHouseholdCommand command);

    void removeMember(UUID partyId, UUID participantId, UUID requesterId);

    List<HouseholdSummary> retrieveByUserId(UUID userId);

    List<UUID> getHouseholdMembersUserIds(UUID userId);

    void disbandHousehold(UUID householdParty, UUID requesterId);

    Household patchHousehold(PatchPartyCommand command);
}
