package com.fabiankevin.app.services;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StatsService {
    StatsSummary getStatsSummary(UUID userId, StatsQuery query);
    List<SummaryPoint> findDailyTotalBalanceByUserIdsAndDateTimeFrom(UUID userId, LocalDate fromDateTime);
}
