package com.fabiankevin.app.models.household;

import lombok.Builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record HouseholdSummary(
        UUID id,
        String name,
        UUID leaderId,
        List<HouseholdMemberSummary> members,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public HouseholdSummary {
        Objects.requireNonNull(leaderId, "leaderId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        members = Optional.ofNullable(members).orElse(new ArrayList<>());
    }
}
