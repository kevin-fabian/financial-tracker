package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SummaryPoint;

import java.math.BigDecimal;

public record SummaryPointResponse(
        String label,
        BigDecimal total) {
    public static SummaryPointResponse from(SummaryPoint point) {
        return new SummaryPointResponse(point.label(), point.total());
    }
}
