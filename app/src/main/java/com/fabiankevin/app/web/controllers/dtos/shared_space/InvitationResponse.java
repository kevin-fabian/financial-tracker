package com.fabiankevin.app.web.controllers.dtos.shared_space;

import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.shared_space.InvitationSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing an invitation to a shared space")
public record InvitationResponse(
    @Schema(description = "Invitation identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Inviter display name", example = "John Doe")
    String inviterName,

    @Schema(description = "Inviter initials", example = "JD")
    String inviterInitial,

    @Schema(description = "Invitee display name", example = "Jane Doe")
    String inviteeName,

    @Schema(description = "Invitee initials", example = "JD")
    String inviteeInitial,

    @Schema(description = "Proposed sharing mode name", example = "Mutual Sharing")
    String proposedSharingModeName,

    @Schema(description = "Proposed sharing mode description")
    String proposedSharingModeDescription,

    @Schema(description = "Proposed role name", example = "Read & Write")
    String proposedRoleName,

    @Schema(description = "Proposed role description")
    String proposedRoleDescription,

    @Schema(description = "Status of the invitation", example = "PENDING")
    InvitationStatus status,

    @Schema(description = "Timestamp when the invitation was created")
    Instant createdAt,

    @Schema(description = "Timestamp when the invitation expires")
    Instant expiresAt,

    @Schema(description = "Shared space identifier", example = "a1b2c3d4-...")
    UUID sharedSpaceId,

    @Schema(description = "Display name of the shared space", example = "Family 2026 Budget")
    String sharedSpaceName
) {
    public static InvitationResponse from(InvitationSummary invitation) {
        return InvitationResponse.builder()
            .id(invitation.id())
            .inviterName(invitation.inviterName())
            .inviterInitial(invitation.inviterInitial())
            .inviteeName(invitation.inviteeName())
            .inviteeInitial(invitation.inviteeInitial())
            .proposedSharingModeName(invitation.proposedSharingModeName())
            .proposedSharingModeDescription(invitation.proposedSharingModeDescription())
            .proposedRoleName(invitation.proposedRoleName())
            .proposedRoleDescription(invitation.proposedRoleDescription())
            .status(invitation.status())
            .createdAt(invitation.createdAt())
            .expiresAt(invitation.expiresAt())
            .sharedSpaceId(invitation.sharedSpaceId())
            .sharedSpaceName(invitation.sharedSpaceName())
            .build();
    }
}
