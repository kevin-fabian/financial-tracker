package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to send an invitation to join a shared space")
public record SendInvitationRequest(
    @NotBlank(message = "Invitee email is required")
    @Email(message = "Invitee email must be a valid email address")
    @Schema(description = "Email of the invited user", example = "jane@example.com")
    String inviteeEmail,

    @Schema(description = "Optional identifier of the invited user when already registered")
    UUID inviteeUserId,

    @NotNull(message = "Proposed role is required")
    @Schema(description = "Access level proposed for the invitee", example = "READ_WRITE")
    AccessLevel proposedRole
) {
    public SendInvitationCommand toCommand(UUID inviterUserId, UUID spaceId) {
        return SendInvitationCommand.builder()
            .inviterUserId(inviterUserId)
            .inviteeEmail(inviteeEmail)
            .inviteeUserId(inviteeUserId)
            .spaceId(spaceId)
            .proposedRole(proposedRole)
            .build();
    }
}
