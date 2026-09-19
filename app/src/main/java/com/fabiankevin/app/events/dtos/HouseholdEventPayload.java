package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.household.HouseholdSummary;

public record HouseholdEventPayload(
        EventAction action,
        HouseholdSummary data
) implements EventPayload<HouseholdSummary> {

    @Override
    public HouseholdSummary payload() {
        return data;
    }
}
