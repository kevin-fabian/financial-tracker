package com.fabiankevin.app.categories.persistence.entities;

import lombok.Builder;

@Builder(toBuilder = true)
public record CategorySummaryProjection(
        CategoryEntity category,
        double amount,
        int totalTransactions
) {
}
