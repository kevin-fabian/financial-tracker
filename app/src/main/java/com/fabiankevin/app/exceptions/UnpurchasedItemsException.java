package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class UnpurchasedItemsException extends DomainException {
    public UnpurchasedItemsException() {
        super("Cannot complete a shopping list with unpurchased items");
    }
}
