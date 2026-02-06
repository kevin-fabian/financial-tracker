package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.services.queries.SummaryQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SummaryRequest(SummaryType type,
                             LocalDate from,
                             LocalDate to) {

    public SummaryQuery toCommand(List<UUID> userIds) {
        return new SummaryQuery(type, from, to, userIds);
    }
}
