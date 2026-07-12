package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class CannotRemoveOwnerException extends ApiException {
    public CannotRemoveOwnerException() {
        super("Cannot remove the space owner", 409);
    }
}
