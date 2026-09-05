package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.AccountResponse;
import com.fabiankevin.app.web.controllers.dtos.AccountSummaryResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.PageResponse;
import com.fabiankevin.app.web.controllers.dtos.PatchAccountRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/accounts", version = "v1")
public class  AccountController {
    private final AccountService accountService;

    @Operation(
            summary = "Retrieve an account",
            description = "Retrieves an account by specified ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Account account = accountService.getAccountById(id, userId);
        return AccountResponse.from(account);
    }

    @Operation(
            summary = "Retrieve account summaries with pagination",
            description = "Retrieves a paginated list of account summaries with aggregated transaction data based on the provided pagination parameters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping
    public PageResponse<AccountSummaryResponse> getAccountSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String month,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());

        YearMonth yearMonth = Optional.ofNullable(month)
                .map(m -> YearMonth.parse(m))
                .orElseGet(YearMonth::now);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        Page<AccountSummary> summaries = accountService.getAccountSummariesByPageQuery(
                new PageQuery(page, size, sort, direction), userId, monthStart, monthEnd);

        return PageResponse.from(Page.<AccountSummaryResponse>builder()
                .content(summaries.content().stream().map(AccountSummaryResponse::from).toList())
                .page(summaries.page())
                .size(summaries.size())
                .totalElements(summaries.totalElements())
                .totalPages(summaries.totalPages())
                .last(summaries.last())
                .first(summaries.first())
                .build());
    }

    @Operation(
            summary = "Create a new account",
            description = "Creates a new account and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Resource created successfully",
                            content = @Content(schema = @Schema(implementation = AccountSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping
    public ResponseEntity<AccountSummaryResponse> createAccount(@Valid @RequestBody CreateAccountRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        AccountSummary created = accountService.createAccountSummary(request.toCommand(userId));
        AccountSummaryResponse response = AccountSummaryResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Patch an account",
            description = "Updates provided fields of an account and returns the updated object",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource updated successfully",
                            content = @Content(schema = @Schema(implementation = AccountSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PatchMapping("/{accountId}")
    public AccountSummaryResponse patchAccount(@PathVariable UUID accountId, @Valid @RequestBody PatchAccountRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        AccountSummary updated = accountService.patchAccountSummary(request.toCommand(accountId, userId));
        return AccountSummaryResponse.from(updated);
    }

    @Operation(
            summary = "Disable an account",
            description = "Disables an account by specified ID. This operation is idempotent.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource disabled successfully",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping("/{accountId}/disable")
    public ResponseEntity<Void> disableAccount(@PathVariable UUID accountId, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        accountService.disableAccount(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
