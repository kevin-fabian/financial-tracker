package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.shared_space.invitations.SendInvitationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to send an invitation to join a shared space")
public record SendInvitationRequest(
    @NotNull(message = "Invitee email is required")
    @Email(message = "Invitee email must be a valid email address")
    @Schema(description = "Email of the invited user", example = "jane@example.com")
    String email
) {
    public SendInvitationCommand toCommand(UUID inviterUserId, UUID spaceId) {
        return SendInvitationCommand.builder()
            .inviterPlayerId(inviterUserId)
            .inviteeEmail(email)
            .partyId(spaceId)
            .build();
    }
}
