package com.fabiankevin.app.services.shopping_list.commands;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record UpdateShoppingListCommand(
        UUID shoppingListId,
        String name,
        String description,
        Double budget,
        List<UUID> sharedWithUserIds,
        UUID userId) {
}
