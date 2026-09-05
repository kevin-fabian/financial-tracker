package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.dtos.party.InvitationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/households", version = "v1")
public class InvitationController {
    private final InvitationService invitationService;

    @Operation(
        summary = "List invitations for the authenticated user",
        description = "Retrieves all invitations where the authenticated user is either the isInviter or the invitee.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitations retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvitationResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @GetMapping("/invitations")
    public List<InvitationResponse> getInvitations(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return invitationService.getInvitationsByUserId(userId).stream()
            .map(InvitationResponse::from)
            .toList();
    }

    @Operation(
        summary = "Send an invitation",
        description = "Sends an invitation to join the specified household. Only the household leader can invite.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitation sent successfully",
                content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the household leader can invite"),
            @ApiResponse(responseCode = "404", description = "Not Found - household does not exist"),
            @ApiResponse(responseCode = "409", description = "Conflict - User is already a member"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{householdId}/invitations")
    public ResponseEntity<InvitationResponse> sendInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the household where the invitation is sent") UUID householdId,
        @Valid @RequestBody SendInvitationRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        InvitationResponse response = InvitationResponse.from(invitationService.sendInvitation(request.toCommand(userId, householdId)));
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Accept an invitation",
        description = "Accepts a pending invitation and adds the authenticated user to the household.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitation accepted successfully",
                content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid invitation state"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{householdId}/invitations/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the invitation to accept") UUID invitationId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        InvitationResponse response = InvitationResponse.from(invitationService.acceptInvitation(new AcceptInvitationCommand(invitationId, userId)));
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Reject an invitation",
        description = "Rejects a pending invitation. Only the invited user can reject it.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitation rejected successfully",
                content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the invited user can reject"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid invitation state"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{householdId}/invitations/{invitationId}/reject")
    public ResponseEntity<InvitationResponse> rejectInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the invitation to reject") UUID invitationId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        InvitationResponse response = InvitationResponse.from(invitationService.rejectInvitation(new RejectInvitationCommand(invitationId, userId)));
        return ResponseEntity.ok(response);
    }
}
