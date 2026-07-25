package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.web.controllers.dtos.BudgetResponse;
import com.fabiankevin.app.web.controllers.dtos.BudgetSummaryResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateBudgetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
@RequestMapping(value = "/api/budgets", version = "v1")
public class BudgetController {
    private final BudgetService budgetService;

    @Operation(
            summary = "Create a new budget",
            description = "Creates a new budget for the authenticated user and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Budget created successfully",
                            content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Budget created = budgetService.createBudget(request.toCommand(userId));
        BudgetResponse response = BudgetResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Retrieve budgets",
            description = "Retrieves a list of budget summaries with aggregated spending for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public List<BudgetSummaryResponse> getBudgets(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        List<BudgetSummary> summaries = budgetService.getBudgetsByUserId(userId);
        return summaries.stream().map(BudgetSummaryResponse::from).toList();
    }
}
