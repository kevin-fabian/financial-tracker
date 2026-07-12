package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record SpaceParticipant(
        UUID id,
        UUID userId,
        AccessLevel accessLevel,
        ParticipantStatus status,
        Instant joinedAt,
        SharingRule sharingRule // null = use space's defaultSharingRule or mode-based default
) {
    public SpaceParticipant {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(accessLevel, "accessLevel");
        Objects.requireNonNull(status, "status");
    }
}
