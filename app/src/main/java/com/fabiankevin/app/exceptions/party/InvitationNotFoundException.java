package com.fabiankevin.app.exceptions.party;

import com.fabiankevin.app.exceptions.NotFoundException;

public class InvitationNotFoundException extends NotFoundException {
    public InvitationNotFoundException() {
        super("Invitation not found");
    }
}
