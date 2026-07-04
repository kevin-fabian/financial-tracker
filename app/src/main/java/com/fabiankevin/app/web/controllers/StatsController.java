package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.StatsService;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/stats", version = "v1")
public class StatsController {
    private final StatsService statsService;

    @Operation(
            summary = "Retrieve financial statistics",
            description = "Returns total balance, income, expenses, and growth percentage for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Statistics retrieved successfully",
                            content = @Content(schema = @Schema(implementation = StatsResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public StatsResponse getStats(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        StatsQuery query = StatsQuery.builder()
                .fromDate(from)
                .toDate(to)
                .accountId(accountId)
                .categoryId(categoryId)
                .build();
        return statsService.getStats(userId, query);
    }
}
