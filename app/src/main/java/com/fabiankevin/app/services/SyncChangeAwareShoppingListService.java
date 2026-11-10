package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.services.shopping_list.commands.CompleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingListCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeAwareShoppingListService implements ShoppingListService {
    private final ShoppingListService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareShoppingListService(
            @Qualifier("defaultShoppingListService")
            ShoppingListService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        delegate.deleteAllByUserId(userId);
    }

    @Override
    public ShoppingListSummary createShoppingList(CreateShoppingListCommand command) {
        ShoppingListSummary result = delegate.createShoppingList(command);
        registerCreateChange(result, command.userId());
        return result;
    }

    @Override
    public ShoppingListSummary completeShoppingList(CompleteShoppingListCommand command) {
        ShoppingListSummary result = delegate.completeShoppingList(command);
        registerUpdateChange(result, command.userId());
        return result;
    }

    @Override
    public ShoppingListSummary updateShoppingList(UpdateShoppingListCommand command) {
        ShoppingListSummary result = delegate.updateShoppingList(command);
        registerUpdateChange(result, command.userId());
        return result;
    }

    @Override
    public ShoppingItemSummary addShoppingItem(CreateShoppingItemCommand command) {
        ShoppingListSummary listSummary = retrieveListSummary(command.shoppingListId(), command.addedBy());
        ShoppingItemSummary result = delegate.addShoppingItem(command);
        if (listSummary != null) {
            registerUpdateChange(listSummary, command.addedBy());
        }
        return result;
    }

    @Override
    public ShoppingItemSummary updateShoppingItem(UpdateShoppingItemCommand command) {
        ShoppingListSummary listSummary = retrieveListSummary(command.shoppingListId(), command.userId());
        ShoppingItemSummary result = delegate.updateShoppingItem(command);
        if (listSummary != null) {
            registerUpdateChange(listSummary, command.userId());
        }
        return result;
    }

    @Override
    public void deleteShoppingItem(DeleteShoppingItemCommand command) {
        ShoppingListSummary listSummary = retrieveListSummary(command.shoppingListId(), command.userId());
        delegate.deleteShoppingItem(command);
        if (listSummary != null) {
            registerUpdateChange(listSummary, command.userId());
        }
    }

    @Override
    public void deleteShoppingList(DeleteShoppingListCommand command) {
        ShoppingListSummary existing = retrieveListSummary(command.shoppingListId(), command.userId());
        delegate.deleteShoppingList(command);
        registerDeleteChange(existing, command.userId());
    }

    @Override
    public List<ShoppingListSummary> getShoppingListsByUserId(UUID userId) {
        return delegate.getShoppingListsByUserId(userId);
    }

    private void registerCreateChange(ShoppingListSummary summary, UUID userId) {
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.SHOPPING_LISTS)
                .entityId(summary.id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(toJson(summary))
                .build());
    }

    private void registerUpdateChange(ShoppingListSummary summary, UUID userId) {
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.SHOPPING_LISTS)
                .entityId(summary.id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(toJson(summary))
                .build());
    }

    private void registerDeleteChange(ShoppingListSummary summary, UUID userId) {
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.SHOPPING_LISTS)
                .entityId(summary.id())
                .operation(SyncChangeOperationType.DELETE)
                .payload(toJson(summary))
                .build());
    }

    private ShoppingListSummary retrieveListSummary(UUID shoppingListId, UUID userId) {
        List<ShoppingListSummary> lists = delegate.getShoppingListsByUserId(userId);
        return lists.stream()
                .filter(s -> s.id().equals(shoppingListId))
                .findFirst()
                .orElse(null);
    }

    private String toJson(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload for sync change", e);
        }
    }
}
