package com.fabiankevin.app.categories.domain.exceptions;

import com.fabiankevin.app.exceptions.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException() {
        super("Category is not found");
    }
}
