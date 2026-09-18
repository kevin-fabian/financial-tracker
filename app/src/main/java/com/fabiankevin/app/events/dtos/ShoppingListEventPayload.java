package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.shopping_list.ShoppingList;

public record ShoppingListEventPayload(
        EventAction action,
        ShoppingList data
) implements EventPayload<ShoppingList> {

    @Override
    public ShoppingList payload() {
        return data;
    }
}
