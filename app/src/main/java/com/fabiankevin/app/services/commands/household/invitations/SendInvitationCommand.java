package com.fabiankevin.app.services.commands.household.invitations;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record SendInvitationCommand(
        UUID inviterUserId,
        String inviteeEmail,
        UUID householdId
) {
    public SendInvitationCommand {
        Objects.requireNonNull(inviterUserId, "Inviter ID cannot be null");
        Objects.requireNonNull(inviteeEmail, "Invitee email cannot be null");
        Objects.requireNonNull(householdId, "Household ID cannot be null");
    }
}
