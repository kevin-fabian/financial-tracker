package com.fabiankevin.app.models.household;

import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record HouseholdMember(
        UUID id,
        UUID userId,
        AccessLevel accessLevel,
        HouseholdMemberStatus status,
        Instant joinedAt) {
    public HouseholdMember {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
