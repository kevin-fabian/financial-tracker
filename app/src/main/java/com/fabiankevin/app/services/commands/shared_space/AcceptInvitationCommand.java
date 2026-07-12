package com.fabiankevin.app.services.commands.shared_space;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AcceptInvitationCommand(
        UUID invitationId,
        UUID acceptingUserId
) {
}
