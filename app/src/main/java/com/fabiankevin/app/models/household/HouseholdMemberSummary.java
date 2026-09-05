package com.fabiankevin.app.models.household;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record HouseholdMemberSummary(
        UUID id,
        User user,
        boolean householdLeader,
        HouseholdMemberStatus status,
        Instant joinedAt) {
    public HouseholdMemberSummary {
        Objects.requireNonNull(status, "status");
    }
}
