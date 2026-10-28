package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.services.shopping_list.commands.CompleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingListCommand;

import java.util.List;
import java.util.UUID;

public interface ShoppingListService {
    void deleteAllByUserId(UUID userId);

    ShoppingListSummary createShoppingList(CreateShoppingListCommand command);

    ShoppingListSummary completeShoppingList(CompleteShoppingListCommand command);

    ShoppingListSummary updateShoppingList(UpdateShoppingListCommand command);

    ShoppingItemSummary addShoppingItem(CreateShoppingItemCommand command);

    ShoppingItemSummary updateShoppingItem(UpdateShoppingItemCommand command);

    void deleteShoppingItem(DeleteShoppingItemCommand command);

    void deleteShoppingList(DeleteShoppingListCommand command);

    List<ShoppingListSummary> getShoppingListsByUserId(UUID userId);
}
