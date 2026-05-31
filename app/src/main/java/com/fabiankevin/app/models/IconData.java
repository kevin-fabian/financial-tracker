package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record IconData(
        UUID id,
        int codePoint,
        String fontFamily,
        String iconName,
        Instant createdAt) {
}
