package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;

import java.util.List;
import java.util.UUID;

public interface ShoppingListService {
    ShoppingListSummary createShoppingList(CreateShoppingListCommand command);

    ShoppingItemSummary addShoppingItem(CreateShoppingItemCommand command);

    List<ShoppingListSummary> getShoppingListsByUserId(UUID userId);
}
