package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.AccessLevel;
import com.fabiankevin.app.models.enums.InvitationStatus;
import com.fabiankevin.app.models.enums.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.InvitationStatus.PENDING;

@Builder
public record Invitation(
        UUID id,
        UUID inviterUserId,
        String inviteeEmail,
        UUID inviteeUserId,       // null if invitee not yet registered
        SharingMode proposedSharingMode,
        AccessLevel proposedRole,
        SharingRule proposedSharingRule,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt,
        UUID resultingSharedSpaceId
) {
    public Invitation {
        Objects.requireNonNull(inviterUserId, "inviterUserId");
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