package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.household.Invitation;

public record InvitationEvent(
        EventAction action,
        Invitation data
) implements DomainEvent<Invitation> {

    @Override
    public Invitation payload() {
        return data;
    }
}
