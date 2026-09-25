package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.dtos.ShoppingListEventPayload;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.exceptions.EmptyShoppingListException;
import com.fabiankevin.app.exceptions.ShoppingItemNotFoundException;
import com.fabiankevin.app.exceptions.ShoppingListNotFoundException;
import com.fabiankevin.app.exceptions.UnpurchasedItemsException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.services.shopping_list.commands.CompleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingListCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingListCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DefaultShoppingListService implements ShoppingListService {
    private final ShoppingListRepository shoppingListRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final EventPublisher shoppingListEventPublisher;

    @Transactional
    @Override
    public ShoppingListSummary createShoppingList(CreateShoppingListCommand command) {
        if (command.name().isBlank()) {
            throw new IllegalArgumentException("Household name is required");
        }

        Instant now = Instant.now();
        Category category = categoryRepository.findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(CategoryNotFoundException::new);

        ShoppingList shoppingList = ShoppingList.builder()
                .name(command.name())
                .description(command.description())
                .category(category)
                .status(ShoppingListStatus.ACTIVE)
                .userId(command.userId())
                .sharedWithUserIds(command.sharedWithUserIds())
                .budget(command.budget())
                .createdAt(now)
                .updatedAt(now)
                .updatedBy(User.of(command.userId()))
                .build();

        ShoppingList saved = shoppingListRepository.save(shoppingList);
        ShoppingListSummary summary = toSummary(saved);
        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.SHOPPING_LIST_CREATED,
                command.userId().toString(),
                summary
        ));
        return summary;
    }

    @Transactional
    @Override
    public ShoppingListSummary completeShoppingList(CompleteShoppingListCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        if (existing.items().isEmpty()) {
            throw new EmptyShoppingListException();
        }

        if (!existing.hasAllItemsPurchased()) {
            throw new UnpurchasedItemsException();
        }

        Instant now = Instant.now();
        ShoppingList completed = existing.toBuilder()
                .status(ShoppingListStatus.COMPLETED)
                .finalAmount(command.finalAmount())
                .completedAt(now)
                .updatedAt(now)
                .updatedBy(User.of(command.userId()))
                .build();

        ShoppingList saved = shoppingListRepository.save(completed);
        ShoppingListSummary summary = toSummary(saved);
        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.SHOPPING_LIST_COMPLETED,
                command.userId().toString(),
                summary
        ));
        return summary;
    }

    @Transactional
    @Override
    public ShoppingListSummary updateShoppingList(UpdateShoppingListCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        UUID resolvedCategoryId = Optional.ofNullable(command.categoryId())
                .orElse(shoppingListRepository.findCategoryById(command.shoppingListId())
                        .map(Category::id)
                        .orElse(null));
        Category category = Optional.ofNullable(resolvedCategoryId)
                .flatMap(id -> categoryRepository.findByIdAndUserId(id, command.userId()))
                .orElse(null);

        ShoppingList patched = existing.toBuilder()
                .name(Optional.ofNullable(command.name()).orElse(existing.name()))
                .description(Optional.ofNullable(command.description()).orElse(existing.description()))
                .budget(Optional.ofNullable(command.budget()).orElse(existing.budget()))
                .category(category)
                .updatedAt(Instant.now())
                .updatedBy(User.of(command.userId()))
                .build();

        ShoppingList saved = shoppingListRepository.save(patched);
        ShoppingListSummary summary = toSummary(saved, category);
        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.SHOPPING_LIST_UPDATED,
                saved.userId().toString(),
                summary
        ));
        return summary;
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

        ShoppingItem savedItem = saved.items().stream()
                .filter(i -> i.createdAt().equals(item.createdAt()))
                .findFirst()
                .orElse(item);

        User user = userClient.getUsersByIds(List.of(savedItem.addedBy()))
                .stream()
                .findFirst()
                .orElse(null);

        ShoppingItemSummary result = ShoppingItemSummary.builder()
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

        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.ITEM_ADDED,
                saved.userId().toString(),
                toSummary(saved.toBuilder()
                        .items(List.of(savedItem))
                        .build())
        ));
        return result;
    }

    @Transactional
    @Override
    public ShoppingItemSummary updateShoppingItem(UpdateShoppingItemCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        ShoppingItem existingItem = existing.items().stream()
                .filter(item -> item.id().equals(command.itemId()))
                .findFirst()
                .orElseThrow(ShoppingItemNotFoundException::new);

        ShoppingItem patched = existingItem.toBuilder()
                .name(Optional.ofNullable(command.name()).orElse(existingItem.name()))
                .category(Optional.ofNullable(command.category()).orElse(existingItem.category()))
                .quantity(Optional.ofNullable(command.quantity()).orElse(existingItem.quantity()))
                .unit(Optional.ofNullable(command.unit()).orElse(existingItem.unit()))
                .price(Optional.ofNullable(command.price()).orElse(existingItem.price()))
                .notes(Optional.ofNullable(command.notes()).orElse(existingItem.notes()))
                .priority(Optional.ofNullable(command.priority()).orElse(existingItem.priority()))
                .purchased(Optional.ofNullable(command.purchased()).orElse(existingItem.purchased()))
                .updatedAt(Instant.now())
                .build();

        List<ShoppingItem> updatedItems = existing.items().stream()
                .map(item -> item.id().equals(command.itemId()) ? patched : item)
                .toList();

        ShoppingList updated = existing.toBuilder()
                .items(updatedItems)
                .updatedAt(Instant.now())
                .build();

        ShoppingList saved = shoppingListRepository.save(updated);
        ShoppingItem savedItem = saved.items().stream()
                .filter(item -> item.id().equals(command.itemId()))
                .findFirst()
                .orElseThrow();

        User user = userClient.getUsersByIds(List.of(savedItem.addedBy()))
                .stream()
                .findFirst()
                .orElse(null);

        ShoppingItemSummary result = toItemSummary(savedItem, user);

        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.ITEM_UPDATED,
                saved.userId().toString(),
                toSummary(saved)
        ));
        return result;
    }

    @Transactional
    @Override
    public void deleteShoppingItem(DeleteShoppingItemCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        if (!existing.removeItem(command.itemId())) {
            throw new ShoppingItemNotFoundException();
        }

        ShoppingList updated = existing.toBuilder()
                .updatedAt(Instant.now())
                .build();

        ShoppingList saved = shoppingListRepository.save(updated);

        shoppingListEventPublisher.publish(saved.userId(), new ShoppingListEventPayload(
                EventAction.ITEM_REMOVED,
                saved.userId().toString(),
                toSummary(saved)
        ));
    }

    @Transactional
    @Override
    public void deleteShoppingList(DeleteShoppingListCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        shoppingListRepository.deleteById(command.shoppingListId());

        shoppingListEventPublisher.publish(existing.userId(), new ShoppingListEventPayload(
                EventAction.SHOPPING_LIST_DELETED,
                existing.userId().toString(),
                toSummary(existing)
        ));
    }

    @Transactional
    @Override
    public List<ShoppingListSummary> getShoppingListsByUserId(UUID userId) {
        List<ShoppingList> result = shoppingListRepository.findAllByUserId(userId);

        List<UUID> addedByIds = result.stream()
                .flatMap(list -> list.items().stream())
                .map(ShoppingItem::addedBy)
                .distinct()
                .toList();
        List<UUID> ownerIds = result.stream()
                .map(ShoppingList::userId)
                .distinct()
                .toList();
        List<UUID> allIds = new ArrayList<>(addedByIds);
        allIds.addAll(ownerIds);
        allIds = allIds.stream().distinct().toList();
        Map<UUID, User> usersById = userClient.getUsersByIds(allIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return result.stream()
                .map(shoppingList -> toSummary(shoppingList, usersById, usersById.get(shoppingList.userId())))
                .toList();
    }

    private ShoppingListSummary toSummary(ShoppingList shoppingList, Map<UUID, User> usersById, User user) {
        List<ShoppingItemSummary> items = shoppingList.items().stream()
                .map(item -> toItemSummary(item, usersById.get(item.addedBy())))
                .toList();

        User updatedBy = Optional.ofNullable(shoppingList.updatedBy())
                .map(User::id)
                .map(usersById::get)
                .orElse(null);

        return ShoppingListSummary.builder()
                .id(shoppingList.id())
                .name(shoppingList.name())
                .description(shoppingList.description())
                .category(shoppingList.category())
                .status(shoppingList.status())
                .items(items)
                .user(user)
                .updatedBy(updatedBy)
                .budget(shoppingList.budget())
                .finalAmount(shoppingList.finalAmount())
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
        Set<UUID> userIds = new java.util.HashSet<>();
        shoppingList.items().forEach(item -> userIds.add(item.addedBy()));
        userIds.add(shoppingList.userId());
        Map<UUID, User> usersById = userClient.getUsersByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));
        return toSummary(shoppingList, usersById, usersById.get(shoppingList.userId()));
    }

    private ShoppingListSummary toSummary(ShoppingList shoppingList, Category category) {
        Set<UUID> userIds = new java.util.HashSet<>();
        shoppingList.items().forEach(item -> userIds.add(item.addedBy()));
        userIds.add(shoppingList.userId());
        Map<UUID, User> usersById = userClient.getUsersByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));
        ShoppingList restored = shoppingList.toBuilder().category(category).build();
        return toSummary(restored, usersById, usersById.get(shoppingList.userId()));
    }
}
