package com.fabiankevin.app.services.commands.party.invitations;

import java.util.UUID;

public record RejectInvitationCommand(
        UUID invitationId,
        UUID inviteeUserId
) {
}
