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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void createCategory_givenValidCommand_thenShouldSaveCategory() {
        UUID userId = UUID.randomUUID();
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .build();

        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("FOOD", created.name(), "name should match command");
        assertEquals(TransactionType.EXPENSE, created.type(), "type should match command");
        assertEquals(userId, created.userId(), "userIds should be set from command");
        verify(categoryRepository, times(1)).save(any());
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
    void getCategoriesByPageQuery_givenNullType_shouldReturnExpenseCategories() {
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

        when(categoryRepository.findAllByPageQuery(query, userId, TransactionType.EXPENSE))
                .thenReturn(expectedPage);

        Page<Category> result = categoryService.getCategoriesByPageQuery(query, userId, type);

        assertEquals(expectedPage, result, "service should return the page provided by repository");
        verify(categoryRepository, times(1)).findAllByPageQuery(any(PageQuery.class), eq(userId), eq(TransactionType.EXPENSE));
    }

    @Test
    void patchCategory_givenValidCommand_thenShouldUpdateName() {
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
}
