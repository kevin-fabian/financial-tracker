package com.fabiankevin.app.web.controllers.dtos.household;

import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.InvitationSummary;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing an invitation to a shared space")
public record InvitationResponse(
    @Schema(description = "Invitation identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Inviter user details", exampleClasses =  UserResponse.class)
    UserResponse inviter,

    @Schema(description = "Invitee user details", exampleClasses =  UserResponse.class)
    UserResponse invitee,

    @Schema(description = "Status of the invitation", example = "PENDING")
    InvitationStatus status,

    @Schema(description = "Timestamp when the invitation was created", example = "2026-01-15T10:30:00Z")
    Instant createdAt,

    @Schema(description = "Timestamp when the invitation expires", example = "2026-02-15T10:30:00Z")
    Instant expiresAt,

    @Schema(description = "Household details you are being invited", exampleClasses = HouseholdMemberResponse.class)
    HouseholdResponse household,

    @Schema(description = "Whether the authenticated user is the inviter", example = "true")
    boolean isInviter
) {
    public static InvitationResponse from(InvitationSummary invitation) {
        return InvitationResponse.builder()
            .id(invitation.id())
            .inviter(UserResponse.from(invitation.inviter()))
            .invitee(UserResponse.from(invitation.invitee()))
            .status(invitation.status())
            .createdAt(invitation.createdAt())
            .expiresAt(invitation.expiresAt())
            .household(HouseholdResponse.from(invitation.household()))
            .isInviter(invitation.isInviter())
            .build();
    }
}
