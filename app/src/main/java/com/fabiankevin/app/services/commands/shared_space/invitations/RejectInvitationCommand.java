package com.fabiankevin.app.services.commands.shared_space.invitations;

import java.util.UUID;

public record RejectInvitationCommand(
        UUID invitationId,
        UUID inviteeUserId
) {
}
