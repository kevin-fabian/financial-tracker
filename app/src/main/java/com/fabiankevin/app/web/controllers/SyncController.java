package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/sync", version = "v1")
public class SyncController {
    private final SyncService syncService;

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
        String subject = jwtAuthenticationToken.getToken().getSubject();
        UUID userId = UUID.fromString(subject);
        log.debug("Sync prepare requested by user: {}", userId);
        syncService.prepare(userId);
        return ResponseEntity.ok().build();
    }
}
