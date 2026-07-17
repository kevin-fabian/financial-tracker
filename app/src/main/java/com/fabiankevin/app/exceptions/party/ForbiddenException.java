package com.fabiankevin.app.exceptions.party;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(message, 403);
    }
}
