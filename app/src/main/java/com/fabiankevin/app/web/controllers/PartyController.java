package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.services.PartyService;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizePartyRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PartyResponse;
import com.fabiankevin.app.web.controllers.dtos.party.PatchPartyRequest;
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
@RequestMapping(value = "/api/parties", version = "v1")
public class PartyController {
    private final PartyService partyService;

    @Operation(
        summary = "Create a party",
        description = "Creates a new party owned by the authenticated user and returns it.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Party created successfully",
                content = @Content(schema = @Schema(implementation = PartyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PostMapping
    public ResponseEntity<PartyResponse> organizeParty(
        @Valid @RequestBody OrganizePartyRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Party created = partyService.organize(request.toCommand(userId));
        PartyResponse response = PartyResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
        summary = "List parties for the authenticated user",
        description = "Retrieves all parties the authenticated user participates in.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Parties retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = PartyResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @GetMapping
    public List<PartyResponse> getParties(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return partyService.retrieveByUserId(userId).stream()
            .map(PartyResponse::from)
            .toList();
    }

    @Operation(
        summary = "Patch a party",
        description = "Updates the name and/or sharing mode of the party owned by the authenticated user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Party updated successfully",
                content = @Content(schema = @Schema(implementation = PartyResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can update the party"),
            @ApiResponse(responseCode = "404", description = "Not Found - Party not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @PatchMapping("/{partyId}")
    public PartyResponse patchParty(
        @PathVariable @NotNull @Schema(description = "ID of the party to update") UUID partyId,
        @RequestBody PatchPartyRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Party updated = partyService.patchParty(request.toCommand(partyId, userId));
        return PartyResponse.from(updated);
    }

    @Operation(
        summary = "Disband a party",
        description = "Deletes the party owned by the authenticated user.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Party deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can delete the party"),
            @ApiResponse(responseCode = "404", description = "Not Found - Party not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> disbandParty(
        @PathVariable @NotNull @Schema(description = "ID of the party to delete") UUID partyId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        partyService.disbandParty(partyId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Kick a party member",
        description = "Kick a party member from the party. Only party leader can kick a member.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Party member has been kicked successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not allowed to remove this party member"),
            @ApiResponse(responseCode = "409", description = "Conflict - Cannot remove the party owner"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
        }
    )
    @DeleteMapping("/{partyId}/partyMembers/{partyMemberId}")
    public ResponseEntity<Void> kickPartyMember(
        @PathVariable @NotNull @Schema(description = "ID of the party") UUID partyId,
        @PathVariable @NotNull @Schema(description = "ID of the participant to remove") UUID partyMemberId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        partyService.kickPartyMember(partyId, partyMemberId, userId);
        return ResponseEntity.noContent().build();
    }
}
