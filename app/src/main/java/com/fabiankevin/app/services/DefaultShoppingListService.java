package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.ShoppingListNotFoundException;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultShoppingListService implements ShoppingListService {
    private final ShoppingListRepository shoppingListRepository;
    private final UserClient userClient;

    @Transactional
    @Override
    public ShoppingListSummary createShoppingList(CreateShoppingListCommand command) {
        Instant now = Instant.now();
        ShoppingList shoppingList = ShoppingList.builder()
                .name(command.name())
                .description(command.description())
                .status(ShoppingListStatus.ACTIVE)
                .userId(command.userId())
                .sharedWithUserIds(command.sharedWithUserIds())
                .budget(command.budget())
                .createdAt(now)
                .updatedAt(now)
                .build();

        ShoppingList saved = shoppingListRepository.save(shoppingList);
        return toSummary(saved);
    }

    @Transactional
    @Override
    public ShoppingItemSummary addShoppingItem(CreateShoppingItemCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        Instant now = Instant.now();
        ShoppingItem item = ShoppingItem.builder()
                .name(command.name())
                .category(command.category())
                .quantity(command.quantity())
                .unit(command.unit())
                .price(command.price())
                .purchased(false)
                .priority(command.priority())
                .notes(command.notes())
                .addedBy(command.addedBy())
                .createdAt(now)
                .updatedAt(now)
                .build();

        existing.addItem(item);
        ShoppingList updated = existing.toBuilder()
                .updatedAt(now)
                .build();

        ShoppingList saved = shoppingListRepository.save(updated);
        ShoppingItem savedItem = saved.items().get(saved.items().size() - 1);

        User user = userClient.getUsersByIds(List.of(savedItem.addedBy()))
                .stream()
                .findFirst()
                .orElse(null);

        return ShoppingItemSummary.builder()
                .id(savedItem.id())
                .name(savedItem.name())
                .category(savedItem.category())
                .quantity(savedItem.quantity())
                .unit(savedItem.unit())
                .price(savedItem.price())
                .purchased(savedItem.purchased())
                .priority(savedItem.priority())
                .notes(savedItem.notes())
                .addedBy(user)
                .createdAt(savedItem.createdAt())
                .updatedAt(savedItem.updatedAt())
                .build();
    }

    @Override
    public List<ShoppingListSummary> getShoppingListsByUserId(UUID userId) {
        List<ShoppingList> shoppingLists = shoppingListRepository.findAllByUserId(userId);

        List<UUID> addedByIds = shoppingLists.stream()
                .flatMap(list -> list.items().stream())
                .map(ShoppingItem::addedBy)
                .distinct()
                .toList();
        List<UUID> userIds = new ArrayList<>(addedByIds);
        userIds.add(userId);
        Map<UUID, User> usersById = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));
        User user = usersById.get(userId);

        return shoppingLists.stream()
                .map(shoppingList -> toSummary(shoppingList, user, usersById))
                .toList();
    }

    private ShoppingListSummary toSummary(ShoppingList shoppingList, User user, Map<UUID, User> usersById) {
        List<ShoppingItemSummary> items = shoppingList.items().stream()
                .map(item -> toItemSummary(item, usersById.get(item.addedBy())))
                .toList();

        return ShoppingListSummary.builder()
                .id(shoppingList.id())
                .name(shoppingList.name())
                .description(shoppingList.description())
                .status(shoppingList.status())
                .items(items)
                .user(user)
                .budget(shoppingList.budget())
                .completedAt(shoppingList.completedAt())
                .createdAt(shoppingList.createdAt())
                .updatedAt(shoppingList.updatedAt())
                .build();
    }

    private ShoppingItemSummary toItemSummary(ShoppingItem item, User addedBy) {
        return ShoppingItemSummary.builder()
                .id(item.id())
                .name(item.name())
                .category(item.category())
                .quantity(item.quantity())
                .unit(item.unit())
                .price(item.price())
                .purchased(item.purchased())
                .priority(item.priority())
                .notes(item.notes())
                .addedBy(addedBy)
                .createdAt(item.createdAt())
                .updatedAt(item.updatedAt())
                .build();
    }

    private ShoppingListSummary toSummary(ShoppingList shoppingList) {
        User user = userClient.getUsersByIds(List.of(shoppingList.userId()))
                .stream()
                .findFirst()
                .orElse(null);
        Map<UUID, User> usersById = user != null ? Map.of(user.id(), user) : Map.of();
        return toSummary(shoppingList, user, usersById);
    }
}
