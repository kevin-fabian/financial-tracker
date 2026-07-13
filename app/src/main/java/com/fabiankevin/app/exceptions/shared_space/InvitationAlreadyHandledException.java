package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public final class InvitationAlreadyHandledException extends ApiException {
    public InvitationAlreadyHandledException() {
        super("Invitation has already been handled", 409);
    }
}
