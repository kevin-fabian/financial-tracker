package com.fabiankevin.app.events.dtos;

import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;

public record ShoppingListEventPayload(
        EventAction action,
        String userId,
        ShoppingListSummary data
) implements EventPayload<ShoppingListSummary> {

    @Override
    public ShoppingListSummary payload() {
        return data;
    }
}
