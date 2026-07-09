package com.fabiankevin.app.clients.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(UUID id, String firstName, String lastName) {
}
