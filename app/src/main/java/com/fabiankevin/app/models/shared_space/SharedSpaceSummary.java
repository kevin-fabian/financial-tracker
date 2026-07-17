package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
public record SharedSpaceSummary(
        UUID id,
        String spaceName,
        UUID ownerUserId,
        List<SpaceParticipantSummary> participants,
        SharingMode sharingMode,
        List<SharedResource> sharedResources,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public SharedSpaceSummary {
        Objects.requireNonNull(ownerUserId, "partyLeaderId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(spaceName, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        sharedResources = Optional.ofNullable(sharedResources).orElse(new ArrayList<>());
        participants = Optional.ofNullable(participants).orElse(new ArrayList<>());
    }
}
