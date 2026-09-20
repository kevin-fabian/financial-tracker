package com.fabiankevin.app.models.household;

import com.fabiankevin.app.models.User;
import lombok.Builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Household(
        UUID id,
        String name,
        UUID leaderId,
        List<HouseholdMember> members,
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

    public HouseholdSummary toSummary(Map<UUID, User> usersById) {
        List<HouseholdMemberSummary> householdMemberSummaries = members.stream()
                .map(member -> {
                    User user = usersById.get(member.userId());
                    boolean leader = leaderId.equals(member.userId());
                    return HouseholdMemberSummary.builder()
                            .id(member.id())
                            .user(user)
                            .householdLeader(leader)
                            .status(member.status())
                            .joinedAt(member.joinedAt())
                            .build();
                })
                .toList();

        return HouseholdSummary.builder()
                .id(id)
                .name(name)
                .leaderId(leaderId)
                .members(householdMemberSummaries)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
