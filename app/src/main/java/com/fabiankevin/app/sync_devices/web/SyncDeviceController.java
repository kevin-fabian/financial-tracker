package com.fabiankevin.app.sync_devices.web;

import com.fabiankevin.app.sync_devices.domain.models.SyncDevice;
import com.fabiankevin.app.sync_devices.domain.services.SyncDeviceService;
import com.fabiankevin.app.sync_devices.web.dtos.SyncDeviceResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateSyncDeviceRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchSyncDeviceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/sync-devices", version = "v1")
public class SyncDeviceController {

    private static final String USER_DEVICE_CLAIM = "userDevice";

    private final SyncDeviceService syncDeviceService;

    @Operation(
            summary = "Prepare data synchronization",
            description = "Triggers data synchronization for the authenticated user. Deletes all user-owned data (transactions, budgets, shopping lists, accounts, categories) to prepare for a fresh import.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Synchronization prepared successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping("/prepare")
    public ResponseEntity<Void> prepareSync(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        log.debug("Sync prepare requested by user: {}", userId);
        syncDeviceService.prepare(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Create an initial sync state",
            description = "Posts an initial sync state for a resource type on a user's device.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Sync state created successfully",
                            content = @Content(schema = @Schema(implementation = SyncDeviceResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping
    public ResponseEntity<SyncDeviceResponse> createSyncDevice(@Valid @RequestBody CreateSyncDeviceRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        String userDevice = getUserDevice(jwtAuthenticationToken);
        log.debug("Sync state creation requested by user: {}, device: {}", userId, userDevice);
        SyncDevice created = syncDeviceService.create(request.toCommand(userId, userDevice));
        return ResponseEntity.created(URI.create("/api/sync-devices/" + created.id()))
                .body(SyncDeviceResponse.from(created));
    }

    @Operation(
            summary = "Patch a sync state",
            description = "Updates the sync status of an existing sync state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Sync state updated successfully",
                            content = @Content(schema = @Schema(implementation = SyncDeviceResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Sync state not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PatchMapping("/{syncId}")
    public SyncDeviceResponse patchSyncDevice(@PathVariable UUID syncId, @Valid @RequestBody PatchSyncDeviceRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        log.debug("Sync state patch requested by user: {} for syncId: {}", userId, syncId);
        SyncDevice updated = syncDeviceService.patch(request.toCommand(syncId, userId));
        return SyncDeviceResponse.from(updated);
    }

    @Operation(
            summary = "Retrieve all sync states",
            description = "Retrieves all sync states for the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Sync states retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SyncDeviceResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid authentication",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping
    public List<SyncDeviceResponse> getAllSyncDevices(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = getUserId(jwtAuthenticationToken);
        log.debug("Sync states retrieval requested by user: {}", userId);
        return syncDeviceService.getAllByUserId(userId).stream()
                .map(SyncDeviceResponse::from)
                .toList();
    }

    private UUID getUserId(JwtAuthenticationToken token) {
        return UUID.fromString(token.getToken().getSubject());
    }

    private String getUserDevice(JwtAuthenticationToken token) {
        return token.getToken().getClaimAsString(USER_DEVICE_CLAIM);
    }
}
