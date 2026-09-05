package com.fabiankevin.app.services.commands.household.invitations;

import java.util.UUID;

public record RejectInvitationCommand(
        UUID invitationId,
        UUID rejectingUserId
) {
}
