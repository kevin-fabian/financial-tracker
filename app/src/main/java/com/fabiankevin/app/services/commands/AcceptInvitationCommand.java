package com.fabiankevin.app.services.commands;

import java.util.UUID;

public record AcceptInvitationCommand(
        UUID invitationId,
        UUID acceptingUserId
) {
}
