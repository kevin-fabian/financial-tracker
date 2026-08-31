package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.persistence.entities.BudgetEntity;
import lombok.Builder;

@Builder(toBuilder = true)
public record BudgetSummaryProjection(
        BudgetEntity budget,
        double spent) {
}
