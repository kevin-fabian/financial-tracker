package com.fabiankevin.app.exceptions;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException() {
        super("Category is not found");
    }
}
