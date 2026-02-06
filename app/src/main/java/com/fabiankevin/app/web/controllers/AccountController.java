package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.AccountResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.PageResponse;
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
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/accounts", version = "v1")
public class AccountController {
    private final AccountService accountService;

    @Operation(
            summary = "Retrieve an account",
            description = "Retrieves an account by specified ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Account account = accountService.getAccountById(id, userId);
        return AccountResponse.from(account);
    }

    @Operation(
            summary = "Retrieve accounts with pagination",
            description = "Retrieves paginated accounts for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public PageResponse<AccountResponse> getAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Page<Account> accounts = accountService.getAccountsByPageAndUserId(new PageQuery(page, size, sort, direction), userId);
        var mappedPage = new com.fabiankevin.app.models.Page<>(
                accounts.content().stream().map(AccountResponse::from).toList(),
                accounts.page(),
                accounts.size(),
                accounts.totalElements(),
                accounts.totalPages(),
                accounts.last(),
                accounts.first()
        );

        return PageResponse.from(mappedPage);
    }

    @Operation(
            summary = "Create a new account",
            description = "Creates a new account and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Resource created successfully",
                            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Account created = accountService.createAccount(request.toCommand(userId));
        AccountResponse response = AccountResponse.from(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Delete an account",
            description = "Deletes an account by specified ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        accountService.deleteAccountById(id, userId);
        return ResponseEntity.noContent().build();
    }
}
