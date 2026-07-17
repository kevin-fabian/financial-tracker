package com.fabiankevin.app.services.commands.shared_space.invitations;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record SendInvitationCommand(
        UUID inviterPlayerId,
        String inviteeEmail,
        UUID partyId
) {
    public SendInvitationCommand {
        Objects.requireNonNull(inviterPlayerId, "Inviter ID cannot be null");
        Objects.requireNonNull(inviteeEmail, "Invitee email cannot be null");
        Objects.requireNonNull(partyId, "Space ID cannot be null");
    }
}
