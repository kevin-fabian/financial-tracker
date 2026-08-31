package com.fabiankevin.app.web.controllers.dtos.budgets;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record BudgetSummaryResponse(
        UUID id,
        UUID userId,
        UUID user,
        UUID updatedBy,
        Instant updatedAt,
        Instant createdAt,
        String budgetMonth,
        String period,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        List<UUID> members,
        double allocated,
        double spent,
        double spentPercentage) {

    public static BudgetSummaryResponse from(BudgetSummary summary) {
        return BudgetSummaryResponse.builder()
                .id(summary.id())
                .userId(summary.userId())
                .user(summary.user() != null ? summary.user().id() : null)
                .updatedBy(summary.updatedBy() != null ? summary.updatedBy().id() : null)
                .updatedAt(summary.updatedAt())
                .createdAt(summary.createdAt())
                .budgetMonth(summary.budgetMonth())
                .period(summary.period() != null ? summary.period().name() : null)
                .categoryId(summary.categoryId())
                .categoryName(summary.categoryName())
                .categoryIcon(summary.categoryIcon())
                .members(summary.members() != null ? summary.members().stream().map(User::id).toList() : null)
                .allocated(summary.allocated())
                .spent(summary.spent())
                .spentPercentage(summary.spentPercentage())
                .build();
    }
}
