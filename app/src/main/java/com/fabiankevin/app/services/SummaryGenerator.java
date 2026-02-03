package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.services.queries.SummaryQuery;

import java.util.List;

public interface SummaryGenerator {
    SummaryType supports();
    List<SummaryPoint> generate(SummaryQuery query);
}
