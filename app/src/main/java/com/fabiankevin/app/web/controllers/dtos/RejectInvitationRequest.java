package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.shared_space.RejectInvitationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to reject an invitation")
public record RejectInvitationRequest(
    @NotBlank(message = "Invitee email is required")
    @Email(message = "A valid invitee email is required")
    @Schema(description = "Email of the invitee rejecting the invitation", example = "jane@example.com")
    String userEmail
) {
    public RejectInvitationCommand toCommand(UUID invitationId) {
        return new RejectInvitationCommand(invitationId, userEmail);
    }
}
