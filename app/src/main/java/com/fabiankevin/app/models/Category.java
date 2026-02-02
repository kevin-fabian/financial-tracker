package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        UUID userId,
        Instant createdAt,
        Instant updatedAt
) {
    public Category {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Category name is required"));
        Optional.ofNullable(userId)
                .orElseThrow(() -> new IllegalArgumentException("User ID is required"));
    }

    public static Category of(String name, UUID userId){
        return Category.builder()
                .name(name)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
