package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class CategoryAlreadyExistException extends ApiException {
    public CategoryAlreadyExistException(String message) {
        super(message);
    }
}
