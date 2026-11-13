package com.fabiankevin.app.sync_devices.web;

import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import com.fabiankevin.app.sync_devices.web.dtos.LatestCursorResponse;
import com.fabiankevin.app.sync_devices.web.dtos.SyncPullChangesRequest;
import com.fabiankevin.app.sync_devices.web.dtos.SyncPullChangesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/sync-changes", version = "v1")
public class SyncChangesController {

    private final SyncChangeService syncChangeService;

    @Operation(
            summary = "Pull sync changes for multiple users",
            description = "Pulls synchronization changes for the specified user cursors. Returns scoped lists of changes per user with pagination metadata.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Sync changes retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User is not a member of the household",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping
    public SyncPullChangesResponse pullChanges(
            @Valid @RequestBody SyncPullChangesRequest request,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        List<SyncCursorQuery> cursorQueries = Optional.ofNullable(request.cursors())
                .map(cursors -> cursors.stream().map(SyncCursorQuery::from).toList())
                .orElse(List.of());
        log.debug("Sync pull changes requested by user: {} with {} cursors", userId, cursorQueries.size());
        List<SyncScope> scopes = syncChangeService.pullChanges(userId, request.primaryLastPulledSequence(), cursorQueries);
        return SyncPullChangesResponse.from(scopes);
    }

    private UUID getUserId(JwtAuthenticationToken token) {
        return UUID.fromString(token.getToken().getSubject());
    }

    @Operation(
            summary = "Get latest sync cursor for a device",
            description = "Retrieves the latest cursor (sequence ID) for the specified user device. The userId is extracted from the access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Latest cursor retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/latest-cursor")
    public LatestCursorResponse getLatestCursor(
            @RequestParam(name = "userDeviceName") String userDeviceName,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        log.debug("Latest cursor requested for user: {} device: {}", userId, userDeviceName);
        Optional<Long> latestCursor = syncChangeService.getLatestCursor(userId);
        return latestCursor.map(LatestCursorResponse::from).orElse(LatestCursorResponse.empty());
    }
}
