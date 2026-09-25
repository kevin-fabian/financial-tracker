package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.dtos.ShoppingListEventPayload;
import com.fabiankevin.app.exceptions.EmptyShoppingListException;
import com.fabiankevin.app.exceptions.InvalidNotesException;
import com.fabiankevin.app.exceptions.ShoppingItemNotFoundException;
import com.fabiankevin.app.exceptions.ShoppingListNotFoundException;
import com.fabiankevin.app.exceptions.UnpurchasedItemsException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.EventAction;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.enums.TransactionType;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultShoppingListServiceTest {
    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventPublisher shoppingListEventPublisher;

    @InjectMocks
    private DefaultShoppingListService shoppingListService;

    @Nested
    class CreateShoppingList {
        @Test
        void givenValidCommand_thenCreatesAndReturnsSummary() {
            UUID userId = UUID.randomUUID();
            CreateShoppingListCommand command = CreateShoppingListCommand.builder()
                    .name("Groceries")
                    .description("Weekly groceries")
                    .userId(userId)
                    .budget(200.0)
                    .build();

            UUID generatedId = UUID.randomUUID();
            when(shoppingListRepository.save(any())).thenAnswer(invocation ->
                    ((ShoppingList) invocation.getArgument(0)).toBuilder().id(generatedId).build()
            );
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));
            when(categoryRepository.findByIdAndUserId(any(), any()))
                    .thenReturn(Optional.of(Category.builder()
                                    .id(command.categoryId())
                                    .name("Groceries")
                                    .type(TransactionType.EXPENSE)
                            .build()));

            ShoppingListSummary created = shoppingListService.createShoppingList(command);

            // identity & ownership
            assertEquals(generatedId, created.id(), "id should be generated");
            assertEquals(userId, created.user().id(), "user should be enriched from UserClient");

            // list fields
            assertEquals("Groceries", created.name(), "name should match command");
            assertEquals("Weekly groceries", created.description(), "description should match command");
            assertEquals(ShoppingListStatus.ACTIVE, created.status(), "status should default to ACTIVE");
            assertEquals(200.0, created.budget(), "budget should match command");
            assertEquals(List.of(), created.items(), "items should be empty on creation");

            // completion & timestamps
            assertNull(created.completedAt(), "completedAt should be null on creation");
            assertNotNull(created.createdAt(), "createdAt should not be null");
            assertNotNull(created.updatedAt(), "updatedAt should not be null");

            // user enrichment
            assertNotNull(created.user(), "user should be enriched");
            assertEquals("John Doe", created.user().fullName(), "user fullName should be enriched");
            assertEquals("JD", created.user().initial(), "user initial should be enriched");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            assertEquals(ShoppingListStatus.ACTIVE, captor.getValue().status());
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.SHOPPING_LIST_CREATED)
            );
        }

        @Test
        void givenBlankName_thenThrowsIllegalArgumentException() {
            CreateShoppingListCommand command = CreateShoppingListCommand.builder()
                    .name(" ")
                    .userId(UUID.randomUUID())
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> shoppingListService.createShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(userClient, never()).getUsersByIds(any());
        }
    }

    @Nested
    class UpdateShoppingList {
        @Test
        void givenValidCommand_thenUpdatesAndReturnsSummary() {
            UUID userId = UUID.randomUUID();
            UUID shoppingListId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Instant now = Instant.now();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Updated Category")
                    .type(TransactionType.EXPENSE)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Old Name")
                    .description("Old description")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .budget(100.0)
                    .category(category)
                    .items(new ArrayList<>())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateShoppingListCommand command = UpdateShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .userId(userId)
                    .name("New Name")
                    .description("New description")
                    .budget(500.0)
                    .categoryId(categoryId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation ->
                    ((ShoppingList) invocation.getArgument(0)).toBuilder().category(category).build()
            );
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Jane").lastName("Smith").build()));
            when(categoryRepository.findByIdAndUserId(eq(categoryId), eq(userId)))
                    .thenReturn(Optional.of(category));

            ShoppingListSummary updated = shoppingListService.updateShoppingList(command);

            // identity & ownership
            assertEquals(shoppingListId, updated.id(), "id should be preserved");
            assertEquals(userId, updated.user().id(), "user should be enriched from UserClient");

            // updated fields
            assertEquals("New Name", updated.name(), "name should be updated");
            assertEquals("New description", updated.description(), "description should be updated");
            assertEquals(500.0, updated.budget(), "budget should be updated");
            assertEquals(category, updated.category(), "category should be updated");

            // unchanged fields
            assertEquals(ShoppingListStatus.ACTIVE, updated.status(), "status should remain ACTIVE");
            assertEquals(List.of(), updated.items(), "items should remain empty");

            // completion fields
            assertNull(updated.finalAmount(), "finalAmount should be null");
            assertNull(updated.completedAt(), "completedAt should be null");

            // timestamps
            assertNotNull(updated.createdAt(), "createdAt should be preserved");
            assertNotNull(updated.updatedAt(), "updatedAt should be updated");

            // user enrichment
            assertNotNull(updated.user(), "user should be enriched");
            assertEquals("Jane Smith", updated.user().fullName(), "user fullName should be enriched");
            assertEquals("JS", updated.user().initial(), "user initial should be enriched");
            assertNotNull(updated.updatedBy(), "updatedBy should be set");
            assertEquals(userId, updated.updatedBy().id(), "updatedBy should be the userId");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            assertEquals("New Name", savedList.name(), "saved name should be updated");
            assertEquals("New description", savedList.description(), "saved description should be updated");
            assertEquals(500.0, savedList.budget(), "saved budget should be updated");
            assertEquals(category, savedList.category(), "saved category should be updated");

            verify(userClient, times(1)).getUsersByIds(List.of(userId));
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.SHOPPING_LIST_UPDATED)
            );
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotSave() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            UpdateShoppingListCommand command = UpdateShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .userId(userId)
                    .name("Updated Name")
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.updateShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class AddShoppingItem {
        @Test
        void givenExistingList_thenAddsItemAndReturnsIt() {
            UUID shoppingListId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(addedBy)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            CreateShoppingItemCommand command = CreateShoppingItemCommand.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .shoppingListId(shoppingListId)
                    .priority(ItemPriority.HIGH)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> {
                ShoppingList s = invocation.getArgument(0);
                List<ShoppingItem> items = new ArrayList<>(s.items());
                ShoppingItem last = items.getLast();
                items.set(items.size() - 1, last.toBuilder().id(UUID.randomUUID()).build());
                return s.toBuilder().items(items).build();
            });
            when(userClient.getUsersByIds(List.of(addedBy)))
                    .thenReturn(List.of(User.builder().id(addedBy).firstName("John").lastName("Doe").build()));

            ShoppingItemSummary added = shoppingListService.addShoppingItem(command);

            // item fields
            assertNotNull(added.id(), "id should be generated");
            assertEquals("Milk", added.name(), "name should match command");
            assertEquals("Dairy", added.category(), "category should match command");
            assertEquals(2.0, added.quantity(), "quantity should match command");
            assertEquals("liters", added.unit(), "unit should match command");
            assertEquals(3.5, added.price(), "price should match command");
            assertFalse(added.purchased(), "purchased should default to false");
            assertEquals(ItemPriority.HIGH, added.priority(), "priority should match command");
            assertEquals("Whole milk", added.notes(), "notes should match command");
            assertNotNull(added.addedBy(), "addedBy user should be enriched");
            assertEquals("John", added.addedBy().firstName(), "addedBy first name should be enriched");
            assertEquals("Doe", added.addedBy().lastName(), "addedBy last name should be enriched");
            assertNotNull(added.createdAt(), "createdAt should not be null");
            assertNotNull(added.updatedAt(), "updatedAt should not be null");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            assertEquals(1, captor.getValue().items().size(), "list should contain the added item");
            assertEquals("Milk", captor.getValue().items().getFirst().name());
            verify(shoppingListEventPublisher, times(1)).publish(eq(addedBy), any(ShoppingListEventPayload.class));
        }

        @Test
        void givenListWithExistingItems_thenAddsNewItemAndPublishesOnlyNewItem() {
            UUID shoppingListId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(false)
                    .priority(ItemPriority.MEDIUM)
                    .addedBy(addedBy)
                    .createdAt(now.minusSeconds(300))
                    .updatedAt(now.minusSeconds(300))
                    .build();
            List<ShoppingItem> items = new ArrayList<>();
            items.add(existingItem);

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(addedBy)
                    .items(items)
                    .createdAt(now.minusSeconds(600))
                    .updatedAt(now.minusSeconds(300))
                    .build();

            CreateShoppingItemCommand command = CreateShoppingItemCommand.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .shoppingListId(shoppingListId)
                    .priority(ItemPriority.HIGH)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> {
                ShoppingList s = invocation.getArgument(0);
                return s.toBuilder().items(new ArrayList<>(s.items())).build();
            });
            when(userClient.getUsersByIds(List.of(addedBy)))
                    .thenReturn(List.of(User.builder().id(addedBy).firstName("John").lastName("Doe").build()));

            ShoppingItemSummary added = shoppingListService.addShoppingItem(command);

            // item fields
            assertEquals("Milk", added.name(), "name should match command");
            assertEquals("Dairy", added.category(), "category should match command");
            assertEquals(2.0, added.quantity(), "quantity should match command");
            assertEquals("liters", added.unit(), "unit should match command");
            assertEquals(3.5, added.price(), "price should match command");
            assertFalse(added.purchased(), "purchased should default to false");
            assertEquals(ItemPriority.HIGH, added.priority(), "priority should match command");
            assertEquals("Whole milk", added.notes(), "notes should match command");
            assertNotNull(added.addedBy(), "addedBy user should be enriched");
            assertEquals("John", added.addedBy().firstName(), "addedBy first name should be enriched");
            assertEquals("Doe", added.addedBy().lastName(), "addedBy last name should be enriched");
            assertNotNull(added.createdAt(), "createdAt should not be null");
            assertNotNull(added.updatedAt(), "updatedAt should not be null");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            assertEquals(2, savedList.items().size(), "list should contain both existing and new item");

            // Verify event payload contains only the newly added item
            ArgumentCaptor<ShoppingListEventPayload> payloadCaptor = ArgumentCaptor.forClass(ShoppingListEventPayload.class);
            verify(shoppingListEventPublisher, times(1)).publish(eq(addedBy), payloadCaptor.capture());
            ShoppingListEventPayload publishedPayload = payloadCaptor.getValue();
            assertEquals(EventAction.ITEM_ADDED, publishedPayload.action(), "action should be ITEM_ADDED");
            assertEquals(1, publishedPayload.data().items().size(), "published data should contain only the new item");
            assertEquals("Milk", publishedPayload.data().items().getFirst().name(), "published item should be the newly added one");
        }

        @Test
        void givenNotesExceedsMaxLength_thenThrowsAndDoesNotSave() {
            UUID shoppingListId = UUID.randomUUID();
            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            CreateShoppingItemCommand command = CreateShoppingItemCommand.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .notes("a".repeat(33))
                    .addedBy(UUID.randomUUID())
                    .shoppingListId(shoppingListId)
                    .priority(ItemPriority.HIGH)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(InvalidNotesException.class,
                    () -> shoppingListService.addShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotSave() {
            UUID shoppingListId = UUID.randomUUID();
            CreateShoppingItemCommand command = CreateShoppingItemCommand.builder()
                    .name("Milk")
                    .addedBy(UUID.randomUUID())
                    .shoppingListId(shoppingListId)
                    .priority(ItemPriority.HIGH)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.addShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteShoppingList {
        @Test
        void givenExistingList_thenDeletesAndPublishesEvent() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            DeleteShoppingListCommand command = DeleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            shoppingListService.deleteShoppingList(command);

            ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(shoppingListRepository, times(1)).deleteById(idCaptor.capture());
            assertEquals(shoppingListId, idCaptor.getValue(), "repository should receive the correct id");
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.SHOPPING_LIST_DELETED)
            );
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotDelete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            DeleteShoppingListCommand command = DeleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.deleteShoppingList(command));

            verify(shoppingListRepository, never()).deleteById(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenWrongUser_thenThrowsAndDoesNotDelete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(ownerUserId)
                    .items(new ArrayList<>())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            DeleteShoppingListCommand command = DeleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .userId(wrongUserId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.deleteShoppingList(command));

            verify(shoppingListRepository, never()).deleteById(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class CompleteShoppingList {
        @Test
        void givenAllItemsPurchased_thenCompletesAndReturnsSummary() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(true)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem item2 = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(true)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(List.of(item1, item2))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .finalAmount(6.0)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(userId).firstName("John").lastName("Doe").build(),
                    User.builder().id(addedBy).firstName("Jane").lastName("Smith").build()
            ));

            ShoppingListSummary completed = shoppingListService.completeShoppingList(command);

            // identity & ownership
            assertEquals(shoppingListId, completed.id(), "id should be preserved");
            assertEquals(userId, completed.user().id(), "user should be enriched from UserClient");

            // completion fields
            assertEquals(ShoppingListStatus.COMPLETED, completed.status(), "status should be COMPLETED");
            assertEquals(6.0, completed.finalAmount(), "finalAmount should match command");
            assertNotNull(completed.completedAt(), "completedAt should be set");
            assertNotNull(completed.updatedAt(), "updatedAt should be updated");

            // items preserved
            assertEquals(2, completed.items().size(), "items should be preserved");
            assertTrue(completed.items().stream().allMatch(ShoppingItemSummary::purchased),
                    "all items should remain purchased");

            // user enrichment
            assertNotNull(completed.user(), "user should be enriched");
            assertEquals("John Doe", completed.user().fullName(), "user fullName should be enriched");
            assertEquals("JD", completed.user().initial(), "user initial should be enriched");
            assertNotNull(completed.updatedBy(), "updatedBy should be set");
            assertEquals(userId, completed.updatedBy().id(), "updatedBy should be the userId");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            assertEquals(ShoppingListStatus.COMPLETED, savedList.status(), "saved status should be COMPLETED");
            assertEquals(6.0, savedList.finalAmount(), "saved finalAmount should match command");
            assertNotNull(savedList.completedAt(), "saved completedAt should be set");

            verify(userClient, times(1)).getUsersByIds(any());
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.SHOPPING_LIST_COMPLETED)
            );
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotComplete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .finalAmount(10.0)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.completeShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenWrongUser_thenThrowsAndDoesNotComplete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(ownerUserId)
                    .items(new ArrayList<>())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .finalAmount(10.0)
                    .userId(wrongUserId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.completeShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenEmptyItems_thenThrowsAndDoesNotComplete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .finalAmount(10.0)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(EmptyShoppingListException.class,
                    () -> shoppingListService.completeShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenUnpurchasedItems_thenThrowsAndDoesNotComplete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(true)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem item2 = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(List.of(item1, item2))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(shoppingListId)
                    .finalAmount(10.0)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(UnpurchasedItemsException.class,
                    () -> shoppingListService.completeShoppingList(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class UpdateShoppingItem {
        @Test
        void givenExistingItem_thenUpdatesAndReturnsSummary() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(List.of(existingItem))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .name("Skim Milk")
                    .unit("gallons")
                    .purchased(true)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(addedBy).firstName("Jane").lastName("Smith").build(),
                    User.builder().id(userId).firstName("John").lastName("Doe").build()
            ));

            ShoppingItemSummary updated = shoppingListService.updateShoppingItem(command);

            // identity & ownership
            assertEquals(itemId, updated.id(), "id should be preserved");
            assertEquals(addedBy, updated.addedBy().id(), "addedBy should be preserved");

            // updated fields
            assertEquals("Skim Milk", updated.name(), "name should be updated");
            assertEquals(2.0, updated.quantity(), "quantity should be unchanged");
            assertEquals("gallons", updated.unit(), "unit should be updated");
            assertEquals(3.5, updated.price(), "price should be unchanged");
            assertEquals("Whole milk", updated.notes(), "notes should be unchanged");
            assertEquals(ItemPriority.HIGH, updated.priority(), "priority should be unchanged");
            assertTrue(updated.purchased(), "purchased should be updated to true");

            // timestamps
            assertNotNull(updated.createdAt(), "createdAt should be preserved");
            assertNotNull(updated.updatedAt(), "updatedAt should be updated");

            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            ShoppingItem savedItem = savedList.items().stream()
                    .filter(i -> i.id().equals(itemId))
                    .findFirst()
                    .orElseThrow();
            assertEquals("Skim Milk", savedItem.name(), "saved name should be updated");
            assertEquals(2.0, savedItem.quantity(), "saved quantity should be unchanged");
            assertTrue(savedItem.purchased(), "saved purchased should be true");

            verify(userClient, times(1)).getUsersByIds(List.of(addedBy));
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.ITEM_UPDATED)
            );
        }

        @Test
        void givenListWithExistingItems_thenPublishesOnlyUpdatedItem() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            UUID otherItemId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem otherItem = ShoppingItem.builder()
                    .id(otherItemId)
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .addedBy(addedBy)
                    .createdAt(now.minusSeconds(300))
                    .updatedAt(now.minusSeconds(300))
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(List.of(existingItem, otherItem))
                    .createdAt(now.minusSeconds(600))
                    .updatedAt(now)
                    .build();

            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .purchased(true)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(addedBy).firstName("Jane").lastName("Smith").build(),
                    User.builder().id(userId).firstName("John").lastName("Doe").build()
            ));

            ShoppingItemSummary updated = shoppingListService.updateShoppingItem(command);

            // Verify returned item
            assertEquals(itemId, updated.id(), "id should be preserved");
            assertTrue(updated.purchased(), "purchased should be updated to true");
            assertEquals("Milk", updated.name(), "name should be unchanged");

            // Verify event payload contains only the updated item
            ArgumentCaptor<ShoppingListEventPayload> payloadCaptor = ArgumentCaptor.forClass(ShoppingListEventPayload.class);
            verify(shoppingListEventPublisher, times(1)).publish(eq(userId), payloadCaptor.capture());
            ShoppingListEventPayload publishedPayload = payloadCaptor.getValue();
            assertEquals(EventAction.ITEM_UPDATED, publishedPayload.action(), "action should be ITEM_UPDATED");
            assertEquals(1, publishedPayload.data().items().size(), "published data should contain only the updated item");
            assertEquals("Milk", publishedPayload.data().items().getFirst().name(), "published item should be the updated one");
            assertTrue(publishedPayload.data().items().getFirst().purchased(), "published item should reflect the update");
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotUpdate() {
            UUID shoppingListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .name("Updated Name")
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.updateShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenWrongUser_thenThrowsAndDoesNotUpdate() {
            UUID shoppingListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(ownerUserId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(ownerUserId)
                    .items(List.of(existingItem))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(wrongUserId)
                    .name("Updated Name")
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.updateShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenItemNotFound_thenThrowsAndDoesNotUpdate() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID wrongItemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(List.of(existingItem))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(wrongItemId)
                    .userId(userId)
                    .name("Updated Name")
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingItemNotFoundException.class,
                    () -> shoppingListService.updateShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class DeleteShoppingItem {
        @Test
        void givenExistingItem_thenDeletesAndPublishesEvent() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem item2 = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(item1, item2)))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(userId).firstName("John").lastName("Doe").build(),
                    User.builder().id(addedBy).firstName("Jane").lastName("Smith").build()
            ));

            shoppingListService.deleteShoppingItem(command);

            // verify repository save with updated items
            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            assertEquals(1, savedList.items().size(), "list should contain remaining item");
            assertEquals("Bread", savedList.items().getFirst().name(), "remaining item should be Bread");

            // verify event published with ITEM_REMOVED action
            verify(shoppingListEventPublisher, times(1)).publish(
                    eq(userId),
                    argThat(payload ->
                            payload instanceof ShoppingListEventPayload p
                                    && p.action() == EventAction.ITEM_REMOVED)
            );
        }

        @Test
        void givenListWithMultipleItems_thenPublishesOnlyDeletedItem() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID otherItemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem item2 = ShoppingItem.builder()
                    .id(otherItemId)
                    .name("Bread")
                    .category("Bakery")
                    .quantity(1.0)
                    .unit("loaf")
                    .price(2.5)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .addedBy(addedBy)
                    .createdAt(now.minusSeconds(300))
                    .updatedAt(now.minusSeconds(300))
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(item1, item2)))
                    .createdAt(now.minusSeconds(600))
                    .updatedAt(now)
                    .build();

            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(userId).firstName("John").lastName("Doe").build(),
                    User.builder().id(addedBy).firstName("Jane").lastName("Smith").build()
            ));

            shoppingListService.deleteShoppingItem(command);

            // Verify repository save with remaining items
            ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
            verify(shoppingListRepository, times(1)).save(captor.capture());
            ShoppingList savedList = captor.getValue();
            assertEquals(1, savedList.items().size(), "list should contain remaining item");
            assertEquals("Bread", savedList.items().getFirst().name(), "remaining item should be Bread");

            // Verify event payload contains only the deleted item
            ArgumentCaptor<ShoppingListEventPayload> payloadCaptor = ArgumentCaptor.forClass(ShoppingListEventPayload.class);
            verify(shoppingListEventPublisher, times(1)).publish(eq(userId), payloadCaptor.capture());
            ShoppingListEventPayload publishedPayload = payloadCaptor.getValue();
            assertEquals(EventAction.ITEM_REMOVED, publishedPayload.action(), "action should be ITEM_REMOVED");
            assertEquals(1, publishedPayload.data().items().size(), "published data should contain only the deleted item");
            assertEquals("Milk", publishedPayload.data().items().getFirst().name(), "published item should be the deleted one");
            assertEquals(itemId, publishedPayload.data().items().getFirst().id(), "published item id should match deleted item");
        }

        @Test
        void givenListNotFound_thenThrowsAndDoesNotDelete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.empty());

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.deleteShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenWrongUser_thenThrowsAndDoesNotDelete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(ownerUserId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(ownerUserId)
                    .items(List.of(existingItem))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(itemId)
                    .userId(wrongUserId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingListNotFoundException.class,
                    () -> shoppingListService.deleteShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }

        @Test
        void givenItemNotFound_thenThrowsAndDoesNotDelete() {
            UUID shoppingListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID wrongItemId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingItem existingItem = ShoppingItem.builder()
                    .id(UUID.randomUUID())
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(addedBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList existing = ShoppingList.builder()
                    .id(shoppingListId)
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(existingItem)))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(shoppingListId)
                    .itemId(wrongItemId)
                    .userId(userId)
                    .build();

            when(shoppingListRepository.findById(shoppingListId)).thenReturn(Optional.of(existing));

            assertThrows(ShoppingItemNotFoundException.class,
                    () -> shoppingListService.deleteShoppingItem(command));

            verify(shoppingListRepository, never()).save(any());
            verify(shoppingListEventPublisher, never()).publish(any(), any());
        }
    }
}
