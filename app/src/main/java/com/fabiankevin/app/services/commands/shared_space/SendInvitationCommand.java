package com.fabiankevin.app.services.commands.shared_space;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharingRule;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SendInvitationCommand(
        UUID inviterUserId,
        String inviteeEmail,
        UUID inviteeUserId,
        UUID spaceId,
        String spaceName,
        SharingMode sharingMode,
        AccessLevel proposedRole,
        SharingRule proposedSharingRule,
        SharingRule defaultSharingRule
) {
}
