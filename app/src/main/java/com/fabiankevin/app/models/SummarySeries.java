package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.SummaryType;

import java.util.List;

public record SummarySeries(
        SummaryType type,
        List<SummaryPoint> points
) {
}
