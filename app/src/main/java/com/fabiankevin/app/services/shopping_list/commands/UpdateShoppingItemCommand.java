package com.fabiankevin.app.services.shopping_list.commands;

import com.fabiankevin.app.models.enums.ItemPriority;
import lombok.Builder;
import lombok.With;

import java.util.UUID;

@With
@Builder(toBuilder = true)
public record UpdateShoppingItemCommand(
        UUID shoppingListId,
        UUID itemId,
        String name,
        String category,
        Double quantity,
        String unit,
        Double price,
        String notes,
        ItemPriority priority,
        UUID userId) {
}
