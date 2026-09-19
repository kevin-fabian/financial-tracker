package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.dtos.ShoppingListEventPayload;
import com.fabiankevin.app.exceptions.InvalidNotesException;
import com.fabiankevin.app.exceptions.ShoppingListNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
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
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> {
                ShoppingList s = invocation.getArgument(0);
                return s.toBuilder().id(generatedId).build();
            });
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
            verify(shoppingListEventPublisher, times(1)).publish(eq(userId), any());
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
            when(shoppingListRepository.save(any())).thenAnswer(invocation -> {
                ShoppingList s = invocation.getArgument(0);
                return s.toBuilder().category(category).build();
            });
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
            verify(shoppingListEventPublisher, times(1)).publish(eq(userId), any(ShoppingListEventPayload.class));
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
}
