package com.fabiankevin.app.exceptions;

public class CategoryNotFoundException extends BusinessRuleException {
    public CategoryNotFoundException() {
        super("Category is not found");
    }
}
