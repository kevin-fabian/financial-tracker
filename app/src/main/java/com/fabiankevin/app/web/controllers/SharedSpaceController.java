package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.SharedSpaceService;
import com.fabiankevin.app.services.commands.shared_space.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.RejectInvitationCommand;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.dtos.shared_space.CreateSharedSpaceRequest;
import com.fabiankevin.app.web.controllers.dtos.shared_space.InvitationResponse;
import com.fabiankevin.app.web.controllers.dtos.shared_space.SharedSpaceResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/shared-spaces", version = "v1")
public class SharedSpaceController {
    private final SharedSpaceService sharedSpaceService;
    private final InvitationService invitationService;

    @Operation(
        summary = "Create a shared space",
        description = "Creates a new shared space owned by the authenticated user and returns it.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Shared space created successfully",
                content = @Content(schema = @Schema(implementation = SharedSpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping
    public ResponseEntity<SharedSpaceResponse> createSharedSpace(
        @Valid @RequestBody CreateSharedSpaceRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        SharedSpace created = sharedSpaceService.createShare(request.toCommand(userId));
        SharedSpaceResponse response = SharedSpaceResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
        summary = "List shared spaces for the authenticated user",
        description = "Retrieves all shared spaces the authenticated user participates in.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Shared spaces retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = SharedSpaceResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @GetMapping
    public List<SharedSpaceResponse> getSharedSpaces(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return sharedSpaceService.retrieveByUserId(userId).stream()
            .map(SharedSpaceResponse::from)
            .toList();
    }

    @Operation(
        summary = "List invitations for the authenticated user",
        description = "Retrieves all invitations where the authenticated user is either the inviter or the invitee.",
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
        description = "Sends an invitation to join the specified shared space. Only the space owner can invite.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Invitation sent successfully",
                content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the space owner can invite"),
            @ApiResponse(responseCode = "404", description = "Not Found - Shared space does not exist"),
            @ApiResponse(responseCode = "409", description = "Conflict - User is already a participant"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{spaceId}/invitations")
    public ResponseEntity<InvitationResponse> sendInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the shared space where the invitation is sent") UUID spaceId,
        @Valid @RequestBody SendInvitationRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Invitation invitation = invitationService.sendInvitation(request.toCommand(userId, spaceId));
        InvitationResponse response = InvitationResponse.from(invitation);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{invitationId}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
        summary = "Accept an invitation",
        description = "Accepts a pending invitation and adds the authenticated user to the shared space.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitation accepted",
                content = @Content(schema = @Schema(implementation = SharedSpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid invitation state"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{spaceId}/invitations/{invitationId}/accept")
    public SharedSpaceResponse acceptInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the invitation to accept") UUID invitationId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        SharedSpace space = invitationService.acceptInvitation(
            new AcceptInvitationCommand(invitationId, userId));
        return SharedSpaceResponse.from(space);
    }

    @Operation(
        summary = "Reject an invitation",
        description = "Rejects a pending invitation. Only the invited user can reject it.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Invitation rejected",
                content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the invited user can reject"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid invitation state"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping("/{spaceId}/invitations/{invitationId}/reject")
    public InvitationResponse rejectInvitation(
        @PathVariable @NotNull @Schema(description = "ID of the invitation to reject") UUID invitationId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        log.debug("User {} rejecting invitation {}", userId, invitationId);
        Invitation invitation = invitationService.rejectInvitation(
            new RejectInvitationCommand(invitationId, userId));
        return InvitationResponse.from(invitation);
    }

    @Operation(
        summary = "Remove a participant",
        description = "Removes a participant from the shared space. Only the owner or the participant themselves can remove.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Participant removed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not allowed to remove this participant"),
            @ApiResponse(responseCode = "409", description = "Conflict - Cannot remove the space owner"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @DeleteMapping("/{spaceId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
        @PathVariable @NotNull @Schema(description = "ID of the shared space") UUID spaceId,
        @PathVariable @NotNull @Schema(description = "ID of the participant to remove") UUID participantId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        sharedSpaceService.removeParticipant(spaceId, participantId, userId);
        return ResponseEntity.noContent().build();
    }
}
