package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.enums.SummaryType;

import java.util.List;

public record SummarySeriesResponse(
        SummaryType type,
        List<SummaryPointResponse> points
) {
    public static SummarySeriesResponse from(SummarySeries summarySeries) {
        return new SummarySeriesResponse(summarySeries.type(), summarySeries.points().stream()
                .map(SummaryPointResponse::from)
                .toList());
    }
}
