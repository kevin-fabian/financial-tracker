package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
public record PartySummary(
        UUID id,
        String name,
        UUID partyLeaderId,
        List<PartyMemberSummary> partyMembers,
        SharingMode sharingMode,
        List<SharedItem> sharedItems,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public PartySummary {
        Objects.requireNonNull(partyLeaderId, "partyLeaderId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        sharedItems = Optional.ofNullable(sharedItems).orElse(new ArrayList<>());
        partyMembers = Optional.ofNullable(partyMembers).orElse(new ArrayList<>());
    }
}
