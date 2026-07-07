package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.AccountType;
import lombok.Builder;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record AccountSummary(
        UUID id,
        String name,
        List<UUID> userIds,
        Currency currency,
        AccountType type,
        boolean active,
        boolean system,
        double totalBalance,
        double percentage,
        int totalTransactions) {
    public AccountSummary {
        Optional.ofNullable(userIds).orElseThrow(() -> new IllegalArgumentException("User IDs are required"));
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }
}
