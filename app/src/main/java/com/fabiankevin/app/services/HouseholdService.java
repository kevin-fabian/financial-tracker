package com.fabiankevin.app.services;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.commands.household.OrganizeHouseholdCommand;
import com.fabiankevin.app.services.commands.household.PatchHouseholdCommand;

import java.util.List;
import java.util.UUID;

public interface HouseholdService {
    HouseholdSummary organize(OrganizeHouseholdCommand command);

    void removeMember(UUID partyId, UUID memberId, UUID leaderId);

    List<HouseholdSummary> retrieveByUserId(UUID userId);

    List<UUID> getHouseholdMembersUserIds(UUID userId);

    void disbandHousehold(UUID householdParty, UUID leaderId);

    Household patchHousehold(PatchHouseholdCommand command);
}
