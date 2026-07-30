package com.fabiankevin.app.models.shopping_list;

import com.fabiankevin.app.models.enums.ItemPriority;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record ShoppingItem(
        UUID id,
        String name,
        String category,
        double quantity,
        String unit,
        double price,
        boolean isPurchased,
        UUID purchasedBy,
        Instant purchasedAt,
        ItemPriority priority,
        String notes,
        UUID addedBy,
        Instant createdAt) {
    public ShoppingItem {
        Objects.requireNonNull(id, "ID is required");
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Item name is required"));
        Objects.requireNonNull(priority, "Priority is required");
        Objects.requireNonNull(addedBy, "Added by is required");
        Objects.requireNonNull(createdAt, "Created at is required");
    }
}
