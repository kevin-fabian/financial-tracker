package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncChangeAwareShoppingListServiceTest {

    private final ShoppingListService delegate = mock(ShoppingListService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareShoppingListService service = new SyncChangeAwareShoppingListService(
            delegate, syncChangeService, jsonMapper
    );

    private ShoppingListSummary buildSummary(UUID id, UUID userId) {
        return ShoppingListSummary.builder()
                .id(id)
                .name("Groceries")
                .description("Test list")
                .status(ShoppingListStatus.ACTIVE)
                .items(List.of())
                .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("John").lastName("Doe").build())
                .budget(200.0)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
    }

    @Nested
    class CreateShoppingList {

        @Test
        void givenNewList_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            CreateShoppingListCommand command = CreateShoppingListCommand.builder()
                    .name("Groceries")
                    .description("Weekly groceries")
                    .userId(userId)
                    .budget(200.0)
                    .build();

            ShoppingListSummary expected = buildSummary(listId, userId);
            when(delegate.createShoppingList(command)).thenReturn(expected);

            ShoppingListSummary result = service.createShoppingList(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.SHOPPING_LISTS);
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            ShoppingListSummary capturedSummary = jsonMapper.readValue(change.payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(expected);
        }

        @Test
        void givenListWithZeroBudget_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            CreateShoppingListCommand command = CreateShoppingListCommand.builder()
                    .name("Budget list")
                    .userId(userId)
                    .budget(0.0)
                    .build();

            ShoppingListSummary expected = ShoppingListSummary.builder()
                    .id(listId)
                    .name("Budget list")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .budget(0.0)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.createShoppingList(command)).thenReturn(expected);

            ShoppingListSummary result = service.createShoppingList(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            ShoppingListSummary capturedSummary = jsonMapper.readValue(captor.getValue().payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(expected);
        }
    }

    @Nested
    class UpdateShoppingList {

        @Test
        void givenExistingList_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            UpdateShoppingListCommand command = UpdateShoppingListCommand.builder()
                    .shoppingListId(listId)
                    .name("Updated Groceries")
                    .description("Updated description")
                    .budget(500.0)
                    .userId(userId)
                    .build();

            ShoppingListSummary expected = ShoppingListSummary.builder()
                    .id(listId)
                    .name("Updated Groceries")
                    .description("Updated description")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .budget(500.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.updateShoppingList(command)).thenReturn(expected);

            ShoppingListSummary result = service.updateShoppingList(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            ShoppingListSummary capturedSummary = jsonMapper.readValue(captor.getValue().payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(expected);
        }

        @Test
        void givenListWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            UpdateShoppingListCommand command = UpdateShoppingListCommand.builder()
                    .shoppingListId(listId)
                    .name("Premium list")
                    .budget(1000.0)
                    .userId(userId)
                    .build();

            ShoppingListSummary expected = ShoppingListSummary.builder()
                    .id(listId)
                    .name("Premium list")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .budget(1000.0)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.updateShoppingList(command)).thenReturn(expected);

            ShoppingListSummary result = service.updateShoppingList(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            ShoppingListSummary capturedSummary = jsonMapper.readValue(captor.getValue().payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(expected);
        }
    }

    @Nested
    class CompleteShoppingList {

        @Test
        void givenList_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            CompleteShoppingListCommand command = CompleteShoppingListCommand.builder()
                    .shoppingListId(listId)
                    .finalAmount(185.0)
                    .userId(userId)
                    .build();

            ShoppingListSummary expected = ShoppingListSummary.builder()
                    .id(listId)
                    .name("Groceries")
                    .status(ShoppingListStatus.COMPLETED)
                    .finalAmount(185.0)
                    .completedAt(Instant.parse("2025-06-01T12:00:00Z"))
                    .items(List.of())
                    .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .budget(200.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T12:00:00Z"))
                    .build();

            when(delegate.completeShoppingList(command)).thenReturn(expected);

            ShoppingListSummary result = service.completeShoppingList(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.entityType()).isEqualTo(ResourceType.SHOPPING_LISTS);
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
            ShoppingListSummary capturedSummary = jsonMapper.readValue(change.payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(expected);
        }
    }

    @Nested
    class AddShoppingItem {

        @Test
        void givenExistingList_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            CreateShoppingItemCommand command = CreateShoppingItemCommand.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .addedBy(userId)
                    .shoppingListId(listId)
                    .priority(ItemPriority.HIGH)
                    .build();

            ShoppingListSummary listSummary = buildSummary(listId, userId);
            ShoppingItemSummary itemSummary = ShoppingItemSummary.builder()
                    .id(itemId)
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(com.fabiankevin.app.models.User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(List.of(listSummary));
            when(delegate.addShoppingItem(command)).thenReturn(itemSummary);

            ShoppingItemSummary result = service.addShoppingItem(command);

            assertThat(result).isSameAs(itemSummary);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.SHOPPING_LISTS);
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
        }
    }

    @Nested
    class UpdateShoppingItem {

        @Test
        void givenExistingItem_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UpdateShoppingItemCommand command = UpdateShoppingItemCommand.builder()
                    .shoppingListId(listId)
                    .itemId(itemId)
                    .name("Whole Milk")
                    .userId(userId)
                    .build();

            ShoppingListSummary listSummary = buildSummary(listId, userId);
            ShoppingItemSummary itemSummary = ShoppingItemSummary.builder()
                    .id(itemId)
                    .name("Whole Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(com.fabiankevin.app.models.User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T11:00:00Z"))
                    .build();

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(List.of(listSummary));
            when(delegate.updateShoppingItem(command)).thenReturn(itemSummary);

            ShoppingItemSummary result = service.updateShoppingItem(command);

            assertThat(result).isSameAs(itemSummary);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
        }
    }

    @Nested
    class DeleteShoppingItem {

        @Test
        void givenExistingItem_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            DeleteShoppingItemCommand command = DeleteShoppingItemCommand.builder()
                    .shoppingListId(listId)
                    .itemId(UUID.randomUUID())
                    .userId(userId)
                    .build();

            ShoppingListSummary listSummary = buildSummary(listId, userId);

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(List.of(listSummary));

            service.deleteShoppingItem(command);

            verify(delegate, times(1)).deleteShoppingItem(command);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
        }
    }

    @Nested
    class DeleteShoppingList {

        @Test
        void givenExistingList_shouldDelegateAndRegisterDeleteChange() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            DeleteShoppingListCommand command = DeleteShoppingListCommand.builder()
                    .shoppingListId(listId)
                    .userId(userId)
                    .build();

            ShoppingListSummary existing = buildSummary(listId, userId);

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(List.of(existing));

            service.deleteShoppingList(command);

            verify(delegate, times(1)).deleteShoppingList(command);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.SHOPPING_LISTS);
            assertThat(change.entityId()).isEqualTo(listId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.DELETE);
            ShoppingListSummary capturedSummary = jsonMapper.readValue(change.payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(existing);
        }

        @Test
        void givenListWithCompletedStatus_shouldRegisterDeleteChangeWithFinalState() {
            UUID userId = UUID.randomUUID();
            UUID listId = UUID.randomUUID();
            DeleteShoppingListCommand command = DeleteShoppingListCommand.builder()
                    .shoppingListId(listId)
                    .userId(userId)
                    .build();

            ShoppingListSummary existing = ShoppingListSummary.builder()
                    .id(listId)
                    .name("Completed list")
                    .status(ShoppingListStatus.COMPLETED)
                    .finalAmount(150.0)
                    .completedAt(Instant.parse("2025-05-01T10:00:00Z"))
                    .items(List.of())
                    .user(com.fabiankevin.app.models.User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .budget(200.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-05-01T10:00:00Z"))
                    .build();

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(List.of(existing));

            service.deleteShoppingList(command);

            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            ShoppingListSummary capturedSummary = jsonMapper.readValue(captor.getValue().payload(), ShoppingListSummary.class);
            assertThat(capturedSummary).isEqualTo(existing);
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void getShoppingListsByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            List<ShoppingListSummary> expected = List.of(buildSummary(UUID.randomUUID(), userId));

            when(delegate.getShoppingListsByUserId(userId)).thenReturn(expected);

            List<ShoppingListSummary> result = service.getShoppingListsByUserId(userId);

            assertThat(result).isSameAs(expected);
            verify(delegate, times(1)).getShoppingListsByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteAllByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();

            service.deleteAllByUserId(userId);

            verify(delegate, times(1)).deleteAllByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }
    }
}
