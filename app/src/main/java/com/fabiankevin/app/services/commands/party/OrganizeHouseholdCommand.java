package com.fabiankevin.app.services.commands.party;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record OrganizeHouseholdCommand(
        UUID leaderId,
        String householdName) {
    public OrganizeHouseholdCommand {
        Objects.requireNonNull(leaderId, "Household leader ID cannot be null");
    }
}
