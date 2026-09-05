package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.NotFoundException;

import java.util.UUID;

public final class HouseholdMemberNotFoundException extends NotFoundException {
    private final UUID userId;

    public HouseholdMemberNotFoundException(UUID userId) {
        super("Participant not found for user ID %s".formatted(userId));
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
