package com.fabiankevin.app.models.budgets;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Budget(
        UUID id,
        User user,
        User updatedBy,
        BudgetPeriod period,
        Category category,
        double allocated,
        Instant createdAt,
        Instant updatedAt) {
}
