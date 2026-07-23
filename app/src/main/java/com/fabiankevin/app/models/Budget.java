package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Budget(
        UUID id,
        String name,
        Category category,
        String icon,
        String colorPalette,
        double allocated,
        Instant createdAt,
        Instant updatedAt) {
}
