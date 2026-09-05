package com.fabiankevin.app.services.commands.household.invitations;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AcceptInvitationCommand(
        UUID invitationId,
        UUID acceptingUserId
) {
}
