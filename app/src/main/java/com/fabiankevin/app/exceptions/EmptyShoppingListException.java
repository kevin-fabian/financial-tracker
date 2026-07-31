package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class EmptyShoppingListException extends DomainException {
    public EmptyShoppingListException() {
        super("Cannot complete a shopping list without items");
    }
}
