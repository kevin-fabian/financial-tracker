package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.InvalidNotesException;
import com.fabiankevin.app.exceptions.ShoppingListNotFoundException;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultShoppingListServiceTest {
    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private UserClient userClient;

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
