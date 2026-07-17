package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PartyMember(
        UUID id,
        UUID playerId,
        AccessLevel accessLevel,
        ParticipantStatus status,
        Instant joinedAt) {
    public PartyMember {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }

    public boolean isOwner(UUID ownerId) {
        return playerId == ownerId;
    }
}
