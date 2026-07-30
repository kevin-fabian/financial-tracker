package com.fabiankevin.app.exceptions;

public class ShoppingListNotFoundException extends NotFoundException {
    public ShoppingListNotFoundException() {
        super("Shopping list not found");
    }
}
