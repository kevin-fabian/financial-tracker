package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SummaryPoint;

public record SummaryPointResponse(
        String label,
        double total) {
    public static SummaryPointResponse from(SummaryPoint point) {
        return new SummaryPointResponse(point.label(), point.total());
    }
}
