package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        UUID userId,
        Instant createdAt,
        Instant updatedAt
) {

    public static Category of(String name, UUID userId){
        return Category.builder()
                .name(name)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
