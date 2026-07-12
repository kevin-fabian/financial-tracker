package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.AccessLevel;
import com.fabiankevin.app.models.enums.SharingMode;
import com.fabiankevin.app.models.shared_space.SharingRule;

import java.util.UUID;

public record SendInvitationCommand(
        UUID inviterUserId,
        String inviteeEmail,
        UUID spaceId,
        String spaceName,
        SharingMode sharingMode,
        AccessLevel proposedRole,
        SharingRule proposedSharingRule,
        SharingRule defaultSharingRule
) {
}
