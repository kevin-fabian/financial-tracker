package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Account(
        UUID id,
        String name,
        User user,
        Currency currency,
        Instant createdAt,
        Instant updatedAt
) {
    public Account {
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }
}
