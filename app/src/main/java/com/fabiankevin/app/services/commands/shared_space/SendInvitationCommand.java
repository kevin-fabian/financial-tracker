package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record SendInvitationCommand(
        UUID inviterUserId,
        UUID inviteeUserId,
        UUID spaceId,
        String spaceName,
        SharingMode sharingMode,
        AccessLevel proposedRole
) {
    public SendInvitationCommand {
        Objects.requireNonNull(inviterUserId, "Inviter ID cannot be null");
        Objects.requireNonNull(inviteeUserId, "Invitee user ID cannot be null");
        Objects.requireNonNull(proposedRole, "Proposed role cannot be null");
        Objects.requireNonNull(spaceId, "Space ID cannot be null");
    }
}
