package com.fabiankevin.app.exceptions;

public class ShoppingItemNotFoundException extends NotFoundException {
    public ShoppingItemNotFoundException() {
        super("Shopping item not found");
    }
}
