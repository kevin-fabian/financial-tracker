package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.web.controllers.dtos.party.HouseholdResponse;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PatchHouseholdRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/households", version = "v1")
public class HouseholdController {
    private final HouseholdService householdService;

    @Operation(
        summary = "Create a household",
        description = "Creates a new household owned by the authenticated user and returns it.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Household created successfully",
                content = @Content(schema = @Schema(implementation = HouseholdResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    @PostMapping
    public ResponseEntity<HouseholdResponse> organizeHousehold(
        @Valid @RequestBody OrganizeHouseholdRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        HouseholdSummary created = householdService.organize(request.toCommand(userId));
        HouseholdResponse response = HouseholdResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
        summary = "List households for the authenticated user",
        description = "Retrieves all households the authenticated user participates in.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Households retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = HouseholdResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    @GetMapping
    public List<HouseholdResponse> getHouseholds(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return householdService.retrieveByUserId(userId).stream()
            .map(HouseholdResponse::from)
            .toList();
    }

    @Operation(
        summary = "Patch a household",
        description = "Updates the name and/or sharing mode of the household owned by the authenticated user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - Household updated successfully",
                content = @Content(schema = @Schema(implementation = HouseholdResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can update the household",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Household not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    @PatchMapping("/{householdId}")
    public HouseholdResponse patchHousehold(
        @PathVariable @NotNull @Schema(description = "ID of the household to update") UUID householdId,
        @RequestBody PatchHouseholdRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Household updated = householdService.patchHousehold(request.toCommand(householdId, userId));
        return HouseholdResponse.from(updated);
    }

    @Operation(
        summary = "Disband a household",
        description = "Deletes the household owned by the authenticated user.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Household deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can delete the household",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Household not found", content =  @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> disbandHousehold(
        @PathVariable @NotNull @Schema(description = "ID of the household to delete") UUID householdId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        householdService.disbandHousehold(householdId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Kick a household member",
        description = "Kick a household member from the household. Only household leader can kick a member.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Household member has been kicked successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not allowed to remove this household member",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Cannot remove the household owner",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    @DeleteMapping("/{householdId}/members/{householdMemberId}")
    public ResponseEntity<Void> kickHouseholdMember(
        @PathVariable @NotNull @Schema(description = "ID of the household") UUID householdId,
        @PathVariable @NotNull @Schema(description = "ID of the member to remove") UUID householdMemberId,
        JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        householdService.removeMember(householdId, householdMemberId, userId);
        return ResponseEntity.noContent().build();
    }
}
