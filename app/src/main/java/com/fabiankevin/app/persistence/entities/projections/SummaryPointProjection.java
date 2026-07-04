package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.models.SummaryPoint;

import java.math.BigDecimal;

public record SummaryPointProjection(
        String label,
        BigDecimal total
) {
    public SummaryPointProjection(int label, double total) {
        this(String.valueOf(label), BigDecimal.valueOf(total));
    }

    public SummaryPointProjection(String label, double total) {
        this(label, BigDecimal.valueOf(total));
    }

    public SummaryPoint toModel() {
        return new SummaryPoint(label, total);
    }
}
