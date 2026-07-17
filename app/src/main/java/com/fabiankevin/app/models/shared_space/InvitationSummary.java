package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record InvitationSummary(
        UUID id,
        String inviterName,
        String inviterInitial,
        String inviteeName,
        String inviteeInitial,
        String proposedSharingModeName,
        String proposedSharingModeDescription,
        String proposedRoleName,
        String proposedRoleDescription,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt,
        UUID sharedSpaceId,
        String sharedSpaceName
) {
    public InvitationSummary {
        Objects.requireNonNull(status, "status");
    }
}
