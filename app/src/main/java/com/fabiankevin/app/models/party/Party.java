package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.party.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.*;

@Builder(toBuilder = true)
public record Party(
        UUID id,
        String name, // "Family 2026 Budget", "Trip Expenses"
        UUID partyLeaderId, // Primary owner (can have co-owners)
        List<PartyMember> partyMembers, // Core: Multiple partyMembers with individual roles
        SharingMode sharingMode, // Global sharing mode for the space
        List<SharedItem> sharedItems, // Resources shared into this space
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public Party {
        Objects.requireNonNull(partyLeaderId, "partyLeaderId is required");
        Objects.requireNonNull(sharingMode, "sharingMode is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        sharedItems = Optional.ofNullable(sharedItems).orElse(new ArrayList<>());
        partyMembers = Optional.ofNullable(partyMembers).orElse(new ArrayList<>());
    }
}
