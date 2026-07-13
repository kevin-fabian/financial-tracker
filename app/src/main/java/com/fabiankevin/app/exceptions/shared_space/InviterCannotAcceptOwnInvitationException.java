package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class InviterCannotAcceptOwnInvitationException extends ApiException {
    public InviterCannotAcceptOwnInvitationException() {
        super("Inviter cannot accept their own invitation", 409);
    }
}
