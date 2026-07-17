package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PartyMember(
        UUID id,
        UUID playerId,
        AccessLevel accessLevel,
        PartyMemberStatus status,
        Instant joinedAt) {
    public PartyMember {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
