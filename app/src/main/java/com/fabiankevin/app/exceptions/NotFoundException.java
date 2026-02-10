package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
