package com.fabiankevin.app.services.shopping_list.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record DeleteShoppingItemCommand(
        UUID shoppingListId,
        UUID itemId,
        UUID userId) {
}
