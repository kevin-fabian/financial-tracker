package com.fabiankevin.app.clients.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserClientResponse(UUID id, String firstName, String lastName) {
}
