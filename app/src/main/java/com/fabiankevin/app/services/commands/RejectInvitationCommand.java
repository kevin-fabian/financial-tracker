package com.fabiankevin.app.services.commands;

import java.util.UUID;

public record RejectInvitationCommand(
        UUID invitationId,
        String userEmail
) {
}
