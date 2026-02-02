package com.fabiankevin.app.exceptions;

public class CategoryAlreadyExistException extends BusinessRuleException {
    public CategoryAlreadyExistException(String message) {
        super(message);
    }
}
