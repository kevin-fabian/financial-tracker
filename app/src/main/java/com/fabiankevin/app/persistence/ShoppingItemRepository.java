package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;

import java.util.Set;
import java.util.UUID;

public interface ShoppingItemRepository {
    ShoppingItem save(ShoppingItem shoppingItem, ShoppingList shoppingList);

    ShoppingItem update(ShoppingItem shoppingItem, ShoppingList shoppingList);

    Set<ShoppingItem> findByShoppingListId(UUID shoppingListId);

    long countByShoppingListId(UUID shoppingListId);

    void deleteByShoppingListId(UUID shoppingListId);

    void deleteById(UUID id);
}
