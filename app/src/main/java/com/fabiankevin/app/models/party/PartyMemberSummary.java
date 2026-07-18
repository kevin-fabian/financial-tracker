package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PartyMemberSummary(
        UUID id,
        UUID playerId,
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
