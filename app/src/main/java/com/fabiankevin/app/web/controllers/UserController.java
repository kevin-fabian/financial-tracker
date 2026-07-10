package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.dtos.UserResponse;
import com.fabiankevin.app.services.UserRegistrationService;
import com.fabiankevin.app.web.controllers.dtos.CreateUserRequest;
import com.fabiankevin.app.web.controllers.dtos.CreateUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/users", version = "v1")
@RequiredArgsConstructor
public class UserController {
    private final UserRegistrationService userRegistrationService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns the created user",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - User created successfully",
                            content = @Content(schema = @Schema(implementation = CreateUserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UserResponse user = userRegistrationService.register(request.toCommand());
        CreateUserResponse response = CreateUserResponse.from(user);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{id}")
                .buildAndExpand(user.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
