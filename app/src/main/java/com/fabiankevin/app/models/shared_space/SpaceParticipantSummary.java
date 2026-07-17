package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record SpaceParticipantSummary(
        UUID id,
        String name,
        String initial,
        AccessLevel accessLevel,
        ParticipantStatus status,
        Instant joinedAt) {
    public SpaceParticipantSummary {
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
