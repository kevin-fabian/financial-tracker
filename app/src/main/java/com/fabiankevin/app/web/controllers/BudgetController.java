package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.web.controllers.dtos.budgets.BudgetSummaryResponse;
import com.fabiankevin.app.web.controllers.dtos.budgets.CreateBudgetRequest;
import com.fabiankevin.app.web.controllers.dtos.budgets.PatchBudgetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
@RequestMapping(value = "/api/budgets", version = "v1")
public class BudgetController {
    private final BudgetService budgetService;

    @Operation(
            summary = "Create a new budget",
            description = "Creates a new budget for the authenticated user and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Budget created successfully",
                            content = @Content(schema = @Schema(implementation = BudgetSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping
    public ResponseEntity<BudgetSummaryResponse> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        BudgetSummary created = budgetService.createBudget(request.toCommand(userId));
        BudgetSummaryResponse response = BudgetSummaryResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Patch a budget",
            description = "Updates provided fields of a budget and returns the updated object",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Budget updated successfully",
                            content = @Content(schema = @Schema(implementation = BudgetSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PatchMapping("/{id}")
    public BudgetSummaryResponse patchBudget(
            @PathVariable UUID id,
            @RequestBody PatchBudgetRequest request,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        BudgetSummary updated = budgetService.patchBudget(request.toCommand(id, userId));
        return BudgetSummaryResponse.from(updated);
    }

    @Operation(
            summary = "Delete a budget",
            description = "Deletes a budget by id. Returns 204 on success.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Budget deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable UUID id,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        budgetService.deleteBudgetById(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Retrieve budgets",
            description = "Retrieves a list of budget summaries with aggregated spending for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetSummaryResponse.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping
    public List<BudgetSummaryResponse> getBudgets(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        List<BudgetSummary> summaries = budgetService.getBudgetsByUserId(userId);
        return summaries.stream().map(BudgetSummaryResponse::from).toList();
    }
}
