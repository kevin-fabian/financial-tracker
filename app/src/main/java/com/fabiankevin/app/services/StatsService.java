package com.fabiankevin.app.services;

import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsResponse;

import java.util.UUID;

public interface StatsService {
    StatsResponse getStats(UUID userId, StatsQuery query);
}
