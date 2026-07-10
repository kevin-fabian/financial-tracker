package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.UserProvisioningService;
import com.fabiankevin.app.web.controllers.dtos.ProvisionUserResponse;
import com.fabiankevin.app.web.controllers.dtos.UserCreatedEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/users", version = "v1")
public class UserCreatedEventController {
    private final UserProvisioningService userProvisioningService;

    @Operation(
            summary = "Provision user accounts and categories",
            description = "Initializes default accounts and categories for a newly created user based on their interests specified in the event metadata",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - User provisioned successfully",
                            content = @Content(schema = @Schema(implementation = ProvisionUserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping("/provision")
    public void provisionUser(@Valid @RequestBody UserCreatedEvent event) {
        userProvisioningService.provisionUser(event.userId(), event.metadata().accountInterests(), event.metadata().categoryInterests());
    }
}
