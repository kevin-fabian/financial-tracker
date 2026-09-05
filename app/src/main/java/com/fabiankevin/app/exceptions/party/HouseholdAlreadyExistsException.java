package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class HouseholdAlreadyExistsException extends ApiException {
    public HouseholdAlreadyExistsException() {
        super("User already has a household. Only one household per user is allowed.", 400);
    }
}
