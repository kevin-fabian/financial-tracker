package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class PartyNotExistException extends ApiException {
    public PartyNotExistException() {
        super("Party does not exist", 400);
    }
}
