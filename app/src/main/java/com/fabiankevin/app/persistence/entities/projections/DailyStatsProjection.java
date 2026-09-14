package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.web.controllers.dtos.DailyStatsPoint;

public record DailyStatsProjection(
        String label,
        double expenses,
        double income
) {
    public DailyStatsProjection(int label, double expenses, double income) {
        this(String.valueOf(label), expenses, income);
    }

    public DailyStatsPoint toModel() {
        return DailyStatsPoint.builder()
                .label(label)
                .totalExpenses(expenses)
                .totalIncome(income)
                .build();
    }
}
