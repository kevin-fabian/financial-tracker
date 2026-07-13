package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public final class InvitationExpiredException extends ApiException {
    public InvitationExpiredException() {
        super("Invitation has expired", 409);
    }
}
