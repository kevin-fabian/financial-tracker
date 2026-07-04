package com.fabiankevin.app.models;

import lombok.Builder;

@Builder(toBuilder = true)
public record StatsSummary(
        double totalBalance,
        double totalExpenses,
        double totalIncome,
        double growthPercentage
) {
}
