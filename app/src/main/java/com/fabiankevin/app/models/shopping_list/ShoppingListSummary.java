package com.fabiankevin.app.models.shopping_list;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record ShoppingListSummary(
        UUID id,
        String name,
        String description,
        ShoppingListStatus status,
        List<ShoppingItemSummary> items,
        User user,
        double budget,
        Double finalAmount,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
