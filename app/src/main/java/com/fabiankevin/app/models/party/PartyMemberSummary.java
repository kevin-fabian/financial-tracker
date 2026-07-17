package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.PartyMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PartyMemberSummary(
        UUID id,
        String name,
        String initial,
        boolean partyLeader,
        boolean partyMember,
        AccessLevel accessLevel,
        PartyMemberStatus status,
        Instant joinedAt) {
    public PartyMemberSummary {
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
