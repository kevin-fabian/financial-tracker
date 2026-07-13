package com.fabiankevin.app.services.commands.shared_space;

import java.util.UUID;

public record RejectInvitationCommand(
        UUID invitationId,
        UUID inviteeUserId
) {
}
