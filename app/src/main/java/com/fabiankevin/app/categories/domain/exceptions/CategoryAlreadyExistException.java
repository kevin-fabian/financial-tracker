package com.fabiankevin.app.categories.domain.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class CategoryAlreadyExistException extends ApiException {
    public CategoryAlreadyExistException(String message) {
        super(message, 409);
    }
}
