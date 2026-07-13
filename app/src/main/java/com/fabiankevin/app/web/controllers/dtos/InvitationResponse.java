package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing an invitation to a shared space")
public record InvitationResponse(
    @Schema(description = "Invitation identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Proposed sharing mode", example = "MUTUAL_SHARING")
    SharingMode proposedSharingMode,

    @Schema(description = "Proposed access level", example = "READ_WRITE")
    AccessLevel proposedRole,

    @Schema(description = "Status of the invitation", example = "PENDING")
    InvitationStatus status,

    @Schema(description = "Timestamp when the invitation was created")
    Instant createdAt,

    @Schema(description = "Timestamp when the invitation expires")
    Instant expiresAt,

    @Schema(description = "Target shared space identifier", example = "c3d4e5f6-...")
    UUID sharedSpaceId
) {
    public static InvitationResponse from(Invitation invitation) {
        return InvitationResponse.builder()
            .id(invitation.id())
            .proposedSharingMode(invitation.proposedSharingMode())
            .proposedRole(invitation.proposedRole())
            .status(invitation.status())
            .createdAt(invitation.createdAt())
            .expiresAt(invitation.expiresAt())
            .sharedSpaceId(invitation.sharedSpaceId())
            .build();
    }
}
