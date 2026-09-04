package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.*;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.services.shopping_list.commands.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultShoppingListService implements ShoppingListService {
    private final ShoppingListRepository shoppingListRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;

    @Transactional
    @Override
    public ShoppingListSummary createShoppingList(CreateShoppingListCommand command) {
        if(command.name() == null || command.name().isBlank()) {
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
                .build();

        ShoppingList saved = shoppingListRepository.save(shoppingList);
        return toSummary(saved, category);
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
                .build();

        ShoppingList saved = shoppingListRepository.save(completed);
        return toSummary(saved);
    }

    @Transactional
    @Override
    public ShoppingListSummary updateShoppingList(UpdateShoppingListCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        if (!existing.userId().equals(command.userId())) {
            throw new ShoppingListNotFoundException();
        }

        UUID resolvedCategoryId = command.categoryId() != null
                ? command.categoryId()
                : shoppingListRepository.findCategoryById(command.shoppingListId()).map(Category::id).orElse(null);
        Category category = resolvedCategoryId != null
                ? categoryRepository.findByIdAndUserId(resolvedCategoryId, command.userId()).orElse(null)
                : null;

        ShoppingList patched = existing.toBuilder()
                .name(command.name() != null ? command.name() : existing.name())
                .description(command.description() != null ? command.description() : existing.description())
                .budget(command.budget() != null ? command.budget() : existing.budget())
                .sharedWithUserIds(command.sharedWithUserIds() != null ? command.sharedWithUserIds() : existing.sharedWithUserIds())
                .category(category)
                .updatedAt(Instant.now())
                .build();

        ShoppingList saved = shoppingListRepository.save(patched);
        return toSummary(saved, category);
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

    @Transactional
    @Override
    public ShoppingItemSummary updateShoppingItem(UpdateShoppingItemCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        boolean isOwner = existing.userId().equals(command.userId());
        boolean isSharedWith = existing.sharedWithUserIds().contains(command.userId());
        if (!isOwner && !isSharedWith) {
            throw new ShoppingListNotFoundException();
        }

        ShoppingItem existingItem = existing.items().stream()
                .filter(item -> item.id().equals(command.itemId()))
                .findFirst()
                .orElseThrow(ShoppingItemNotFoundException::new);

        ShoppingItem patched = existingItem.toBuilder()
                .name(command.name() != null ? command.name() : existingItem.name())
                .category(command.category() != null ? command.category() : existingItem.category())
                .quantity(command.quantity() != null ? command.quantity() : existingItem.quantity())
                .unit(command.unit() != null ? command.unit() : existingItem.unit())
                .price(command.price() != null ? command.price() : existingItem.price())
                .notes(command.notes() != null ? command.notes() : existingItem.notes())
                .priority(command.priority() != null ? command.priority() : existingItem.priority())
                .purchased(command.purchased() != null ? command.purchased() : existingItem.purchased())
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

        return toItemSummary(savedItem, user);
    }

    @Transactional
    @Override
    public void deleteShoppingItem(DeleteShoppingItemCommand command) {
        ShoppingList existing = shoppingListRepository.findById(command.shoppingListId())
                .orElseThrow(ShoppingListNotFoundException::new);

        boolean isOwner = existing.userId().equals(command.userId());
        boolean isSharedWith = existing.sharedWithUserIds().contains(command.userId());
        if (!isOwner && !isSharedWith) {
            throw new ShoppingListNotFoundException();
        }

        if (!existing.removeItem(command.itemId())) {
            throw new ShoppingItemNotFoundException();
        }

        ShoppingList updated = existing.toBuilder()
                .updatedAt(Instant.now())
                .build();

        shoppingListRepository.save(updated);
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
    }

    @Transactional
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
                .category(shoppingList.category())
                .status(shoppingList.status())
                .items(items)
                .user(user)
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
        Optional<User> userOpt = userClient.getUsersByIds(List.of(shoppingList.userId()))
                .stream()
                .findFirst();
        Map<UUID, User> usersById = userOpt
                .map(u -> Map.of(u.id(), u))
                .orElseGet(Map::of);
        return toSummary(shoppingList, userOpt.orElse(null), usersById);
    }

    private ShoppingListSummary toSummary(ShoppingList shoppingList, Category category) {
        Optional<User> userOpt = userClient.getUsersByIds(List.of(shoppingList.userId()))
                .stream()
                .findFirst();
        Map<UUID, User> usersById = userOpt
                .map(u -> Map.of(u.id(), u))
                .orElseGet(Map::of);
        ShoppingList restored = shoppingList.toBuilder().category(category).build();
        return toSummary(restored, userOpt.orElse(null), usersById);
    }
}
