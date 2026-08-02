package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository {
    ShoppingList save(ShoppingList shoppingList);

    Optional<ShoppingList> findById(UUID id);

    Optional<UUID> findCategoryIdById(UUID id);

    List<ShoppingList> findAllByUserId(UUID userId);
}
