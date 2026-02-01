package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {

    public static Category of(String name){
        return Category.builder()
                .name(name)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
