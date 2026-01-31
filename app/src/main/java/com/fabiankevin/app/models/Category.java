package com.fabiankevin.app.models;

import java.time.Instant;
import java.util.UUID;


public record Category(
        UUID id,
        String code,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
