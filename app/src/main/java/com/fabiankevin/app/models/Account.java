package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.AccountType;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record Account(
        UUID id,
        String name,
        UUID userId,
        Currency currency,
        AccountType type,
        Instant createdAt,
        Instant updatedAt) {
    public Account {
        Objects.requireNonNull(userId, "User ID is required");
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }
}
