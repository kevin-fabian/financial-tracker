package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.household.InvitationSummary;

public record InvitationEventPayload(
        EventAction action,
        InvitationSummary data
) implements EventPayload<InvitationSummary> {

    @Override
    public InvitationSummary payload() {
        return data;
    }
}
