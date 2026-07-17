package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class CannotRemoveOwnerException extends ApiException {
    public CannotRemoveOwnerException() {
        super("Cannot remove the party leader", 409);
    }
}
