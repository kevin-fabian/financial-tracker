package com.fabiankevin.app.models.shopping_list;

import com.fabiankevin.app.models.enums.ShoppingListStatus;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.*;

@With
@Builder(toBuilder = true)
public record ShoppingList(
        UUID id,
        String name,
        String description,
        ShoppingListStatus status,
        List<ShoppingItem> items,
        UUID userId,
        double budget,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
    public ShoppingList {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Shopping list name is required"));
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(createdAt, "Created at is required");
        Objects.requireNonNull(updatedAt, "Updated at is required");
        items = Optional.ofNullable(items).orElse(new ArrayList<>());
    }

    public void addItem(ShoppingItem item) {
        items.add(item);
    }
}
