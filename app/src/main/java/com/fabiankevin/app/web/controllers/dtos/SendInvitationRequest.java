package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.household.invitations.SendInvitationCommand;
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
    public SendInvitationCommand toCommand(UUID inviterUserId, UUID householdId) {
        return SendInvitationCommand.builder()
            .inviterUserId(inviterUserId)
            .inviteeEmail(email)
            .householdId(householdId)
            .build();
    }
}
