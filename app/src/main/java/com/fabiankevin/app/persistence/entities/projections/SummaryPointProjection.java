package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.models.SummaryPoint;

import java.math.BigDecimal;

public record SummaryPointProjection(
        String label,
        BigDecimal total
) {
    public SummaryPoint toModel() {
        return new SummaryPoint(label, total);
    }
}
