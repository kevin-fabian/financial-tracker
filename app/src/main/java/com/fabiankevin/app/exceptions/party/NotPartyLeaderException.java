package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class NotPartyLeaderException extends ApiException {
    public NotPartyLeaderException() {
        super("Only the party leader can perform this action", 403);
    }
}
