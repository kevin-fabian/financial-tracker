package com.fabiankevin.app.services.shopping_list.commands;

import com.fabiankevin.app.models.enums.ItemPriority;
import lombok.Builder;
import lombok.With;

import java.util.UUID;

@With
@Builder(toBuilder = true)
public record CreateShoppingItemCommand(
        String name,
        String category,
        double quantity,
        String unit,
        double price,
        String notes,
        UUID addedBy,
        UUID shoppingListId,
        ItemPriority priority) {
}
