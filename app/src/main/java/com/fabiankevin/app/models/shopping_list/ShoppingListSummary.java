package com.fabiankevin.app.models.shopping_list;

import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record ShoppingListSummary(
        UUID id,
        String name,
        String description,
        Category category,
        ShoppingListStatus status,
        List<ShoppingItemSummary> items,
        User user,
        User updatedBy,
        double budget,
        Double finalAmount,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {

    public ShoppingList toModel() {
        return ShoppingList.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .status(status)
                .items(Optional.ofNullable(items).stream().flatMap(List::stream)
                        .map(ShoppingItemSummary::toModel)
                        .toList())
                .userId(Optional.ofNullable(user).map(User::id).orElse(null))
                .sharedWithUserIds(new ArrayList<>())
                .budget(budget)
                .finalAmount(finalAmount)
                .completedAt(completedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .updatedBy(updatedBy)
                .build();
    }
}
