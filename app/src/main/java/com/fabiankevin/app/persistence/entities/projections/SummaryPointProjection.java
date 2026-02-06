package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.models.SummaryPoint;

import java.math.BigDecimal;

public record SummaryPointProjection(
        String label,
        BigDecimal total
) {
    public SummaryPointProjection(int label, BigDecimal total) {
        this(String.valueOf(label), total);
    }

    public SummaryPoint toModel() {
        return new SummaryPoint(label, total);
    }
}
