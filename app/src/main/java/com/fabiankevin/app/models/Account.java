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
        Currency currency,
        AccountType type,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        User user) {
    public Account {
        Objects.requireNonNull(user, "User object is required");
        Objects.requireNonNull(user.id(), "User ID is required");
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }
}
