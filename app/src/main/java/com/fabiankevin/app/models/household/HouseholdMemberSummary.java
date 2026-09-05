package com.fabiankevin.app.models.household;

import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record HouseholdMemberSummary(
        UUID id,
        UUID playerId,
        String name,
        String initial,
        boolean partyLeader,
        boolean partyMember,
        AccessLevel accessLevel,
        HouseholdMemberStatus status,
        double pastWeekDailyAverageTransactionCount,
        int activeBudgetCount,
        int activeShoppingListCount,
        Instant joinedAt) {
    public HouseholdMemberSummary {
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
