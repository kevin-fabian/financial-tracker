package com.fabiankevin.app.models.budgets;

import com.fabiankevin.app.models.Category;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Budget(
        UUID id,
        UUID userId,
        UUID updatedBy,
        BudgetPeriod period,
        Category category,
        double allocated,
        Instant createdAt,
        Instant updatedAt) {
}
