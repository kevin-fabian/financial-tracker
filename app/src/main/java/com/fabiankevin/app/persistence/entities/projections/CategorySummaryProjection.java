package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.persistence.entities.CategoryEntity;
import lombok.Builder;

@Builder(toBuilder = true)
public record CategorySummaryProjection(
        CategoryEntity category,
        double amount,
        int totalTransactions
) {
}
