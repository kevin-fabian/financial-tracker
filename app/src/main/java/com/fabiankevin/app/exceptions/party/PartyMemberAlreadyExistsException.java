package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class PartyMemberAlreadyExistsException extends ApiException {
    public PartyMemberAlreadyExistsException() {
        super("User is already a participant in this space", 409);
    }
}
