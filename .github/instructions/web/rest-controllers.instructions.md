---
applyTo: '**/web/controllers/*Controller.java'
description: 'Rest controller implementation requirements.'
---

## Controller implementation requirements

When writing rest controller implementation, please follow these guidelines:

- Place controller classes in the `web.controllers` package.
- Place request and response DTOs in the `web.controllers.dtos` package.
- Use Lombok `@Slf4j` and `@RequiredArgsConstructor` for logging and dependency injections.
- Each file should contain a single class.
- Name the class ending with `Controller`.
- Use `@RestController` and `@RequestMapping` annotations for controller classes.
- Use `@Operation`, `@ApiResponse`, `@Content`, and `@Schema` annotations from `io.swagger.v3.oas.annotations` for documenting the API endpoints.
- Use `@Valid` and validation annotations (e.g., `@NotBlank`, `@Email`) from `jakarta.validation.constraints` for request DTOs.
- Follow RESTful conventions for endpoint design (e.g., use appropriate HTTP methods and status codes).
- The controller class should be concise and focused on handling HTTP requests and responses only.

Examples:

```java
package com.example.web.controllers.dtos;

import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating a new user.")
public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Schema(description = "Name of the user.", example = "John Doe")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid format")
        @Schema(description = "Email address of the user.", example = "john.doe@example.com")
        String email
) {

    public CreateUserCommand toCommand() {
        return CreateUserCommand.builder()
                .name(this.name())
                .email(this.email())
                .build();
    }
}
```

```java
package com.example.web.controllers.dtos;

import com.example.models.User;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing a user record.")
public record UserResponse(
        @Schema(description = "Unique identifier of the user.", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Name of the user.", example = "John Doe")
        String name,
        @Schema(description = "Email address of the user.", example = "john.doe@example.com")
        String email,
        @Schema(description = "Timestamp when the user was created.", example = "2024-06-01T12:34:56.789Z")
        Instant createdAt,
        @Schema(description = "Timestamp when the user was last updated.", example = "2024-06-02T08:21:45.123Z")
        Instant updatedAt
) {

    public static UserResponse from(final User user) {
        return UserResponse.builder()
                .id(user.id())
                .name(user.name())
                .email(user.email())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .build();
    }
}
```

```java
package com.example.web.controllers;

import com.example.services.UserService;
import com.example.web.controllers.dtos.CreateUserRequest;
import com.example.web.controllers.dtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Create a new X record.",
            description = """
                    Creates a new X record and returns the created object.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is created successfully",
                            content = @Content(schema = @Schema(implementation = XResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(request.toCommand()));
    }

    @Operation(
            summary = "Retrieve a X record.",
            description = """
                    Retrieves a X by specified ID.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is retrieved successfully",
                            content = @Content(schema = @Schema(implementation = XResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return UserResponse.from(userService.getUser(id));
    }
}
```