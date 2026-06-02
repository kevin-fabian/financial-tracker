package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.AccountType;
import lombok.Builder;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Account(
        UUID id,
        String name,
        UUID userId,
        Currency currency,
        AccountType type,
        boolean active,
        boolean system,
        Instant createdAt,
        Instant updatedAt) {
    public Account {
        Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID is required"));
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }
}
