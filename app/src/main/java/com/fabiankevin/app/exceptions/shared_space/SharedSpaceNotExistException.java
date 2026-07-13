package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class SharedSpaceNotExistException extends ApiException {
    public SharedSpaceNotExistException() {
        super("Shared space does not exist", 400);
    }
}
