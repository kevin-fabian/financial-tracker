package com.fabiankevin.app.models.household;

import lombok.Builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Household(
        UUID id,
        String name, // "Family 2026 Budget", "Trip Expenses"
        UUID leaderId, // Primary owner (can have co-owners)
        List<HouseholdMember> members, // Core: Multiple members with individual roles
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public Household {
        Objects.requireNonNull(leaderId, "leaderId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        members = Optional.ofNullable(members).orElse(new ArrayList<>());
    }
}
