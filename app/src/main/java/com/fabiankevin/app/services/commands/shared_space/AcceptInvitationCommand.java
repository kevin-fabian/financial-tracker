package com.fabiankevin.app.services.commands.shared_space;

import java.util.UUID;

public record AcceptInvitationCommand(
        UUID invitationId,
        UUID acceptingUserId
) {
}
