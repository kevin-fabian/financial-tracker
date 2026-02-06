package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.web.controllers.dtos.CreateTransactionRequest;
import com.fabiankevin.app.web.controllers.dtos.SummaryRequest;
import com.fabiankevin.app.web.controllers.dtos.SummarySeriesResponse;
import com.fabiankevin.app.web.controllers.dtos.TransactionResponse;
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
@RequestMapping(value = "/api/transactions", version = "v1")
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(
            summary = "Create a new transaction",
            description = "Creates a new transaction and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Resource created successfully",
                            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Transaction created = transactionService.addTransaction(request.toCommand(userId));
        TransactionResponse response = TransactionResponse.from(created);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Summarizes transactions",
            description = "Summarizes transactions based on the provided criteria",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ok - Resource retrieve successfully",
                            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping("/summary")
    public SummarySeriesResponse getSummary(@ModelAttribute SummaryRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return SummarySeriesResponse.from(transactionService.getSummary(request.toCommand(List.of(userId))));
    }
}
