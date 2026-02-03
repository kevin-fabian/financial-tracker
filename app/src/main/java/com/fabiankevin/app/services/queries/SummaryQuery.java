package com.fabiankevin.app.services.queries;

import com.fabiankevin.app.models.enums.SummaryType;

import java.time.LocalDate;
import java.util.UUID;

public record SummaryQuery(
    SummaryType type,
    LocalDate from,
    LocalDate to,
    UUID userId
) {
}
