package com.fabiankevin.app.services.commands;

import java.util.UUID;

public record RevokeInvitationCommand(
        UUID invitationId,
        UUID revokerUserId
) {
}
