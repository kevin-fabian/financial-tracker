package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.services.SharedSpaceService;
import com.fabiankevin.app.web.controllers.dtos.shared_space.CreateSharedSpaceRequest;
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
