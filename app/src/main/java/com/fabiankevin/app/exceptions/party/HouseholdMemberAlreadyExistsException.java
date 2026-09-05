package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class HouseholdMemberAlreadyExistsException extends ApiException {
    public HouseholdMemberAlreadyExistsException() {
        super("User is already a member in this household", 409);
    }
}
