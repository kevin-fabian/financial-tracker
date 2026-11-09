package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
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

class SyncChangeAwareCategoryServiceTest {

    private final CategoryService delegate = mock(CategoryService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareCategoryService service = new SyncChangeAwareCategoryService(
            delegate, syncChangeService, jsonMapper
    );

    @Nested
    class CreateCategory {

        @Test
        void givenNewCategory_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateCategoryCommand command = CreateCategoryCommand.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .icon("🍔")
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .icon("🍔")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            when(delegate.createCategory(command)).thenReturn(category);

            Category result = service.createCategory(command);

            assertThat(result).isSameAs(category);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.CATEGORIES);
            assertThat(change.entityId()).isEqualTo(categoryId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenCategoryWithNoTransactions_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateCategoryCommand command = CreateCategoryCommand.builder()
                    .id(categoryId)
                    .name("Savings")
                    .type(TransactionType.INCOME)
                    .icon("💰")
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Savings")
                    .type(TransactionType.INCOME)
                    .icon("💰")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.createCategory(command)).thenReturn(category);

            Category result = service.createCategory(command);

            assertThat(result).isSameAs(category);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"name\":\"Savings\"");
            assertThat(payload).contains("\"icon\":\"💰\"");
        }
    }

    @Nested
    class PatchCategory {

        @Test
        void givenExistingCategory_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchCategoryCommand command = PatchCategoryCommand.builder()
                    .id(categoryId)
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .icon("🛒")
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .icon("🛒")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            CategorySummary expected = CategorySummary.builder()
                    .category(category)
                    .totalAmount(1200.75)
                    .percentage(35.0)
                    .totalTransactions(45)
                    .build();

            when(delegate.patchCategory(command)).thenReturn(expected);

            CategorySummary result = service.patchCategory(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.CATEGORIES);
            assertThat(change.entityId()).isEqualTo(categoryId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenCategoryWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchCategoryCommand command = PatchCategoryCommand.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .icon("🎬")
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .icon("🎬")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            CategorySummary expected = CategorySummary.builder()
                    .category(category)
                    .totalAmount(350.25)
                    .percentage(12.5)
                    .totalTransactions(18)
                    .build();

            when(delegate.patchCategory(command)).thenReturn(expected);

            CategorySummary result = service.patchCategory(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"name\":\"Entertainment\"");
            assertThat(payload).contains("\"totalAmount\":350.25");
        }
    }

    @Nested
    class DisableCategory {

        @Test
        void givenActiveCategory_shouldDelegateAndRegisterDeleteChange() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("OldCategory")
                    .type(TransactionType.EXPENSE)
                    .icon("📦")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            CategorySummary existing = CategorySummary.builder()
                    .category(category)
                    .totalAmount(75.0)
                    .percentage(3.0)
                    .totalTransactions(5)
                    .build();

            when(delegate.getCategoryById(categoryId, userId)).thenReturn(existing);

            service.disableCategory(categoryId, userId);

            verify(delegate, times(1)).getCategoryById(categoryId, userId);
            verify(delegate, times(1)).disableCategory(categoryId, userId);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.CATEGORIES);
            assertThat(change.entityId()).isEqualTo(categoryId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.DELETE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenCategoryWithTransactions_shouldRegisterDeleteChangeWithCategoryState() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Utilities")
                    .type(TransactionType.EXPENSE)
                    .icon("⚡")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            CategorySummary existing = CategorySummary.builder()
                    .category(category)
                    .totalAmount(2400.0)
                    .percentage(40.0)
                    .totalTransactions(12)
                    .build();

            when(delegate.getCategoryById(categoryId, userId)).thenReturn(existing);

            service.disableCategory(categoryId, userId);

            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"name\":\"Utilities\"");
            assertThat(payload).contains("\"totalAmount\":2400.0");
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void getCategoryById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(id)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .icon("🍔")
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            CategorySummary expected = CategorySummary.builder()
                    .category(category)
                    .totalAmount(100.0)
                    .percentage(10.0)
                    .totalTransactions(5)
                    .build();

            when(delegate.getCategoryById(id, userId)).thenReturn(expected);

            CategorySummary result = service.getCategoryById(id, userId);

            assertThat(result).isSameAs(expected);
            verify(delegate, times(1)).getCategoryById(id, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteCategoryById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.deleteCategoryById(id, userId);

            verify(delegate, times(1)).deleteCategoryById(id, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void createCategories_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            List<CreateCategoryCommand> commands = List.of(
                    CreateCategoryCommand.builder()
                            .id(UUID.randomUUID())
                            .name("Food")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            service.createCategories(commands, userId);

            verify(delegate, times(1)).createCategories(commands, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getCategoriesByPageQuery_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            PageQuery query = PageQuery.builder().page(0).size(10).build();

            when(delegate.getCategoriesByPageQuery(query, userId, TransactionType.EXPENSE))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            service.getCategoriesByPageQuery(query, userId, TransactionType.EXPENSE);

            verify(delegate, times(1)).getCategoriesByPageQuery(query, userId, TransactionType.EXPENSE);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getCategorySummariesByPageQuery_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            PageQuery query = PageQuery.builder().page(0).size(10).build();

            when(delegate.getCategorySummariesByPageQuery(query, userId, TransactionType.INCOME))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            service.getCategorySummariesByPageQuery(query, userId, TransactionType.INCOME);

            verify(delegate, times(1)).getCategorySummariesByPageQuery(query, userId, TransactionType.INCOME);
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
