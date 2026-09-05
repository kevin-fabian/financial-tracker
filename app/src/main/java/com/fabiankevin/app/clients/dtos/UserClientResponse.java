package com.fabiankevin.app.clients.dtos;

import com.fabiankevin.app.models.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UserClientResponse(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "Jane")
        String firstName,
        @Schema(example = "Doe")
        String lastName) {

    public User toModel() {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
