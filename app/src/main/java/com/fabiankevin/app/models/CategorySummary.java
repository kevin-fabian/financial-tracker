package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record CategorySummary(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        boolean active,
        boolean system,
        double totalAmount,
        double percentage,
        int totalTransactions
) {
    public CategorySummary {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Category name is required"));
        Optional.ofNullable(type)
                .orElseThrow(() -> new IllegalArgumentException("Category type is required"));
    }
}
