package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.models.SummaryPoint;

public record SummaryPointProjection(
        String label,
        double total
) {
    public SummaryPointProjection(int label, double total) {
        this(String.valueOf(label), total);
    }

    public SummaryPoint toModel() {
        return new SummaryPoint(label, total);
    }
}
