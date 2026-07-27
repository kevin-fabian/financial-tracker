package com.fabiankevin.app.models.budgets;

import com.fabiankevin.app.models.User;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummary(
        UUID id,
        UUID userId,
        UUID lastUpdatedBy,
        String lastUpdatedByName,
        String firstName,
        String lastName,
        String initial,
        Instant updatedAt,
        Instant createdAt,
        String budgetMonth,
        BudgetPeriod period,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        List<User> members,
        double allocated,
        double spent,
        double spentPercentage) {
}
