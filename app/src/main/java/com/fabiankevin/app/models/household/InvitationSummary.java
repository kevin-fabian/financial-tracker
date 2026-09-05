package com.fabiankevin.app.models.household;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.household.InvitationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record InvitationSummary(
        UUID id,
        User inviter,
        User invitee,
        InvitationStatus status,
        Household household,
        Instant createdAt,
        Instant expiresAt,
        boolean isInviter
) {
    public InvitationSummary {
        Objects.requireNonNull(status, "status");
    }

    public String inviterName() {
        return Optional.ofNullable(inviter)
                .map(User::fullName)
                .orElse(null);
    }

    public String inviteeName() {
        return Optional.ofNullable(invitee)
                .map(User::fullName)
                .orElse(null);
    }

}
