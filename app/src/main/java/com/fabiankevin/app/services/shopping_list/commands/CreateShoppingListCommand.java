package com.fabiankevin.app.services.shopping_list.commands;

import lombok.Builder;
import lombok.With;

import java.util.UUID;

@With
@Builder(toBuilder = true)
public record CreateShoppingListCommand(
        String name,
        String description,
        UUID userId,
        double budget) {
}
