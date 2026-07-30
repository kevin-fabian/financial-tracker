package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingList;

public interface ShoppingListRepository {
    ShoppingList save(ShoppingList shoppingList);
}
