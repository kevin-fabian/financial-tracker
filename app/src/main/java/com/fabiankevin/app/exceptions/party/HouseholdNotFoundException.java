package com.fabiankevin.app.exceptions.party;

import com.fabiankevin.app.exceptions.NotFoundException;

public class HouseholdNotFoundException extends NotFoundException {
    public HouseholdNotFoundException() {
        super("Household not found");
    }
}
