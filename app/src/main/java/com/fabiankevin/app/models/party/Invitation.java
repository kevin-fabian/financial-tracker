package com.fabiankevin.app.models.party;

import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.party.InvitationStatus.PENDING;

@Builder(toBuilder = true)
public record Invitation(
        UUID id,
        UUID inviterPlayerId,
        UUID inviteePlayerId,
        SharingMode proposedSharingMode,
        AccessLevel proposedRole,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt,
        UUID partyId
) {
    public Invitation {
        Objects.requireNonNull(inviterPlayerId, "inviterPlayerId");
        Objects.requireNonNull(inviteePlayerId, "inviteePlayerId");
        Objects.requireNonNull(status, "status");
    }

    public boolean isPending() {
        return PENDING == this.status;
    }

    public boolean isNotPending(){
        return !isPending();
    }

    public boolean isExpired(){
        return expiresAt.isBefore(Instant.now());
    }
}