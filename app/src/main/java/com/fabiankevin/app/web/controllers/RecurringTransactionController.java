package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.web.controllers.dtos.CreateRecurringTransactionRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchRecurringTransactionRequest;
import com.fabiankevin.app.web.controllers.dtos.RecurringSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/recurring-transactions", version = "v1")
public class RecurringTransactionController {
    private final RecurringTransactionService recurringTransactionService;

    @Operation(
            summary = "Create a new recurring transaction",
            description = "Creates a recurring transaction template that can optionally generate automatic transactions on schedule.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Recurring transaction created successfully",
                            content = @Content(schema = @Schema(implementation = RecurringSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringSummaryResponse create(
            @Valid @RequestBody CreateRecurringTransactionRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return RecurringSummaryResponse.from(recurringTransactionService.create(request.toCommand(userId)));
    }

    @Operation(
            summary = "Retrieve recurring transactions",
            description = "Retrieves a list of recurring transaction summaries for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecurringSummaryResponse.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping
    public List<RecurringSummaryResponse> getRecurringTransactions(JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return recurringTransactionService.getRecurringTransactionsByUserId(userId).stream()
                .map(RecurringSummaryResponse::from)
                .toList();
    }

    @Operation(
            summary = "Patch a recurring transaction",
            description = "Updates a recurring transaction by id. All fields are optional.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Recurring transaction updated successfully",
                            content = @Content(schema = @Schema(implementation = RecurringSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PatchMapping("/{id}")
    public RecurringSummaryResponse patchRecurringTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody PatchRecurringTransactionRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return RecurringSummaryResponse.from(recurringTransactionService.updateRecurringTransaction(request.toCommand(userId, id)));
    }

    @Operation(
            summary = "Delete a recurring transaction",
            description = "Deletes a recurring transaction by id. Returns 204 on success.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Recurring transaction deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(
            @PathVariable UUID id,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        recurringTransactionService.deleteRecurringTransactionById(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Trigger processing of due recurring transactions",
            description = "Starts an asynchronous job to process all due recurring transactions. Returns 202 immediately.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Accepted - Processing job started"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping("/process-due")
    public ResponseEntity<Void> processDueRecurringTransactions() {
        recurringTransactionService.processDueRecurringTransactions();
        return ResponseEntity.accepted().build();
    }
}
