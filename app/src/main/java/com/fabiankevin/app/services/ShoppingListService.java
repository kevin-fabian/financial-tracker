package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;

public interface ShoppingListService {
    ShoppingListSummary createShoppingList(CreateShoppingListCommand command);

    ShoppingItem addShoppingItem(CreateShoppingItemCommand command);
}
