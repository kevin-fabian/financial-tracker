package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DefaultCategoryService categoryService;

    @Test
    void createCategory_givenNewCategory_thenShouldSaveCategory() {
        UUID userId = UUID.randomUUID();
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.empty());
        when(categoryRepository.existsByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("FOOD", created.name(), "name should match command");
        assertEquals(TransactionType.EXPENSE, created.type(), "type should match command");
        assertEquals(userId, created.userId(), "userIds should be set from command");
        assertTrue(created.active(), "category should be active");
        assertNull(created.icon(), "icon should be null when not provided");
        verify(categoryRepository, times(1)).findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId);
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_givenNewCategoryWithIcon_thenShouldSaveCategoryWithIcon() {
        UUID userId = UUID.randomUUID();
        String icon = "attach_money";

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(icon)
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.empty());
        when(categoryRepository.existsByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("FOOD", created.name());
        assertEquals(TransactionType.EXPENSE, created.type());
        assertEquals(icon, created.icon());
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_givenInactiveCategory_shouldReactivateAndReturn() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String existingIcon = "restaurant";

        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(existingIcon)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.of(inactiveCategory));
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(categoryId).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals(categoryId, created.id(), "should return existing category id");
        assertEquals("FOOD", created.name());
        assertEquals(TransactionType.EXPENSE, created.type());
        assertTrue(created.active(), "category should be reactivated");
        assertEquals(existingIcon, created.icon(), "should preserve existing icon when command has no icon");
        verify(categoryRepository, times(1)).findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId);
        verify(categoryRepository, times(1)).save(any());
        verify(categoryRepository, never()).existsByNameAndTypeAndUserId(any(), any(), any());
    }

    @Test
    void createCategory_givenInactiveCategoryWithNewIcon_shouldReactivateAndPreserveExistingIcon() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String oldIcon = "restaurant";
        String newIcon = "home";

        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(oldIcon)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("RENT", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.of(inactiveCategory));
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(categoryId).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals(categoryId, created.id(), "should return existing category id");
        assertTrue(created.active(), "category should be reactivated");
        assertEquals(oldIcon, created.icon(), "existing icon should be preserved and not overwritten by command");
        verify(categoryRepository, times(1)).findInactiveByNameAndTypeAndUserId("RENT", TransactionType.EXPENSE, userId);
        verify(categoryRepository, never()).existsByNameAndTypeAndUserId(any(), any(), any());
    }

    @Test
    void createCategory_givenInactiveCategory_shouldPreserveCreatedAtAndUpdateUpdatedAt() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant originalUpdatedAt = Instant.parse("2025-02-01T00:00:00Z");

        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("restaurant")
                .active(false)
                .createdAt(originalCreatedAt)
                .updatedAt(originalUpdatedAt)
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.of(inactiveCategory));
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(categoryId).build();
        });

        Category created = categoryService.createCategory(command);

        assertTrue(created.active(), "category should be reactivated");
        assertEquals(originalCreatedAt, created.createdAt(), "createdAt should be preserved on reactivation");
        assertTrue(created.updatedAt().isAfter(originalUpdatedAt), "updatedAt should be refreshed on reactivation");
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_givenInactiveCategoryWithNullIcon_shouldReactivateAndKeepNullIcon() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(null)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("new_icon")
                .build();

        when(categoryRepository.findInactiveByNameAndTypeAndUserId("FOOD", TransactionType.EXPENSE, userId))
                .thenReturn(Optional.of(inactiveCategory));
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(categoryId).build();
        });

        Category created = categoryService.createCategory(command);

        assertTrue(created.active(), "category should be reactivated");
        assertNull(created.icon(), "null icon should be preserved even when command provides an icon");
        verify(categoryRepository, times(1)).save(any());
        verify(categoryRepository, never()).existsByNameAndTypeAndUserId(any(), any(), any());
    }

    @Test
    void getCategoryById_givenExistingId_thenShouldReturnCategory() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        Category found = categoryService.getCategoryById(id, userId);

        assertEquals("FOOD", found.name(), "name should match saved category");
        assertEquals(TransactionType.EXPENSE, found.type(), "type should match saved category");
        assertEquals(userId, found.userId(), "userIds should be preserved");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
    }

    @Test
    void getCategoryById_givenNonExistingId_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(id, userId));
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
    }

    @Test
    void deleteCategoryById_givenExistingId_thenShouldDelete() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.deleteByIdAndUserId(id, userId)).thenReturn(1);

        categoryService.deleteCategoryById(id, userId);

        verify(categoryRepository, times(1)).deleteByIdAndUserId(id, userId);
    }

    @Test
    void deleteCategoryById_givenNonExistingId_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.deleteByIdAndUserId(id, userId)).thenReturn(0);

        categoryService.deleteCategoryById(id, userId);

        verify(categoryRepository, times(1)).deleteByIdAndUserId(id, userId);
    }

    @Test
    void getCategoriesByPageQuery_givenValidQuery_thenShouldReturnPagedCategories() {
        UUID userId = UUID.randomUUID();
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = TransactionType.EXPENSE;

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Page<Category> expectedPage = new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true);

        when(categoryRepository.findAllByPageQuery(query, userId, type))
                .thenReturn(expectedPage);

        Page<Category> result = categoryService.getCategoriesByPageQuery(query, userId, type);

        // result should be the same instance returned by repository
        assertEquals(expectedPage, result, "service should return the page provided by repository");
        verify(categoryRepository, times(1)).findAllByPageQuery(any(PageQuery.class), eq(userId), eq(type));
    }

    @Test
    void getCategoriesByPageQuery_givenNullType_shouldReturnAllCategories() {
        UUID userId = UUID.randomUUID();
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = null;

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Page<Category> expectedPage = new Page<>(List.of(c1), 0, 2, 1L, 1, true, true);

        when(categoryRepository.findAllByPageQuery(query, userId, null))
                .thenReturn(expectedPage);

        Page<Category> result = categoryService.getCategoriesByPageQuery(query, userId, type);

        assertEquals(expectedPage, result, "service should return the page provided by repository");
        verify(categoryRepository, times(1)).findAllByPageQuery(any(PageQuery.class), eq(userId), eq(null));
    }

    @Test
    void patchCategory_givenExistingCategory_thenShouldUpdateName() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndTypeAndUserId("GROCERIES", TransactionType.EXPENSE, userId)).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.patchCategory(command);

        assertEquals("GROCERIES", updated.name(), "name should be updated");
        assertEquals(TransactionType.EXPENSE, updated.type(), "type should be preserved");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void patchCategory_givenNonExistingId_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("GROCERIES")
                .userId(userId)
                .build();

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.patchCategory(command));
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void patchCategory_givenNewIconString_shouldUpdateCategoryIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String existingIcon = "restaurant";

        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(existingIcon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        String newIcon = "home";

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndTypeAndUserId("GROCERIES", TransactionType.EXPENSE, userId)).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.patchCategory(command);

        assertEquals("GROCERIES", updated.name());
        assertEquals(newIcon, updated.icon(), "icon should be updated to new string");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void patchCategory_givenNullIcon_shouldNotUpdateIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String existingIcon = "restaurant";

        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(existingIcon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndTypeAndUserId("GROCERIES", TransactionType.EXPENSE, userId)).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.patchCategory(command);

        assertEquals("GROCERIES", updated.name());
        assertEquals(existingIcon, updated.icon(), "icon should be preserved when not provided");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void patchCategory_givenNewNameWithDuplicate_shouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndTypeAndUserId("RENT", TransactionType.EXPENSE, userId)).thenReturn(true);

        assertThrows(com.fabiankevin.app.exceptions.CategoryAlreadyExistException.class, () -> categoryService.patchCategory(command));
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteAllByUserId_givenCategories_shouldDeleteAll() {
        UUID userId = UUID.randomUUID();

        categoryService.deleteAllByUserId(userId);

        verify(categoryRepository, times(1)).deleteAllByUserId(userId);
    }

    @Test
    void deleteAllByUserId_givenNoCategories_shouldStillSucceed() {
        UUID userId = UUID.randomUUID();

        categoryService.deleteAllByUserId(userId);

        verify(categoryRepository, times(1)).deleteAllByUserId(userId);
    }

    @Test
    void deleteAllByUserId_isTransactional_shouldRollbackOnException() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.deleteAllByUserId(userId)).thenThrow(new RuntimeException("database error"));

        assertThrows(RuntimeException.class, () -> categoryService.deleteAllByUserId(userId));
        verify(categoryRepository, times(1)).deleteAllByUserId(userId);
    }
}
