package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingItem;

import java.util.Set;
import java.util.UUID;

public interface ShoppingItemRepository {
    ShoppingItem save(ShoppingItem shoppingItem, UUID shoppingListId);

    ShoppingItem update(ShoppingItem shoppingItem, UUID shoppingListId);

    Set<ShoppingItem> findByShoppingListId(UUID shoppingListId);

    long countByShoppingListId(UUID shoppingListId);

    void deleteByShoppingListId(UUID shoppingListId);

    void deleteById(UUID id);
}
