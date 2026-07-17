package com.fabiankevin.app.services.commands.shared_space;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record SendInvitationCommand(
        UUID inviterUserId,
        String inviteeEmail,
        UUID spaceId
) {
    public SendInvitationCommand {
        Objects.requireNonNull(inviterUserId, "Inviter ID cannot be null");
        Objects.requireNonNull(inviteeEmail, "Invitee email cannot be null");
        Objects.requireNonNull(spaceId, "Space ID cannot be null");
    }
}
