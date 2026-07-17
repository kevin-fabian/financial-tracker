package com.fabiankevin.app.exceptions.party;

import com.fabiankevin.app.exceptions.NotFoundException;

public class PartyNotFoundException extends NotFoundException {
    public PartyNotFoundException() {
        super("Party not found");
    }
}
