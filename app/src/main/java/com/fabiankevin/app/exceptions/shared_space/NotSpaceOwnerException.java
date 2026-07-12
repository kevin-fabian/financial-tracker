package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class NotSpaceOwnerException extends ApiException {
    public NotSpaceOwnerException() {
        super("Only the space owner can perform this action", 403);
    }
}
