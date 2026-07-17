package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public final class InvitationExpiredException extends ApiException {
    public InvitationExpiredException() {
        super("Invitation has expired", 409);
    }
}
