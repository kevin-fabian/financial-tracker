package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class NotHouseholdLeaderException extends ApiException {
    public NotHouseholdLeaderException() {
        super("Only the household leader can perform this action", 403);
    }
}
