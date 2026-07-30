package com.fabiankevin.app.models.shopping_list;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record ShoppingItemSummary(
        UUID id,
        String name,
        String category,
        double quantity,
        String unit,
        double price,
        boolean purchased,
        ItemPriority priority,
        String notes,
        User addedBy,
        Instant createdAt,
        Instant updatedAt) {
    public ShoppingItemSummary {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Item name is required"));
        Objects.requireNonNull(priority, "Priority is required");
        Objects.requireNonNull(addedBy, "Added by is required");
        Objects.requireNonNull(createdAt, "Created at is required");
    }
}
