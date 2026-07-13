package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to send an invitation to join a shared space")
public record SendInvitationRequest(
    @NotNull(message = "Invitee user ID is required")
    @Schema(description = "Identifier of the invited user", example = "b2c3d4e5-...")
    UUID inviteeUserId,

    @NotNull(message = "Proposed role is required")
    @Schema(description = "Access level proposed for the invitee", example = "READ_WRITE")
    AccessLevel proposedRole
) {
    public SendInvitationCommand toCommand(UUID inviterUserId, UUID spaceId) {
        return SendInvitationCommand.builder()
            .inviterUserId(inviterUserId)
            .inviteeUserId(inviteeUserId)
            .spaceId(spaceId)
            .proposedRole(proposedRole)
            .build();
    }
}
