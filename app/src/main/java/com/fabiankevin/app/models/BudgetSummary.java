package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummary(
        UUID id,
        String name,
        Category category,
        String icon,
        String colorPalette,
        double allocated,
        double spent,
        Instant createdAt,
        Instant updatedAt) {
}
