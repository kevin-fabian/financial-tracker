package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        boolean active,
        // This flag will be used for pre-added categories
        boolean system,
        Instant createdAt,
        Instant updatedAt
) {
    public Category {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Category name is required"));
        Optional.ofNullable(type)
                .orElseThrow(() -> new IllegalArgumentException("Category type is required"));
        Optional.ofNullable(userId)
                .orElseThrow(() -> new IllegalArgumentException("User ID is required"));
    }

    public static Category of(String name, TransactionType type, UUID userId, String icon){
        return Category.builder()
                .name(name)
                .type(type)
                .userId(userId)
                .icon(icon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
