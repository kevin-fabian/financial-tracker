package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.shared_space.InvitationStatus.PENDING;

@Builder
public record Invitation(
        UUID id,
        UUID inviterUserId,
        UUID inviteeUserId,
        SharingMode proposedSharingMode,
        AccessLevel proposedRole,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt,
        UUID sharedSpaceId
) {
    public Invitation {
        Objects.requireNonNull(inviterUserId, "inviterUserId");
        Objects.requireNonNull(inviteeUserId, "inviteeUserId");
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