package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummaryProjection(
        UUID id,
        UUID userId,
        UUID updatedBy,
        Instant updatedAt,
        Instant createdAt,
        String period,
        double allocated,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        double spent
) {
}
