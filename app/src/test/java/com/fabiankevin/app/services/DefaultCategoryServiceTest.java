package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
                .userId(userId)
                .build();

        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("FOOD", created.name(), "name should match command");
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
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        Category found = categoryService.getCategoryById(id, userId);

        assertEquals("FOOD", found.name(), "name should match saved category");
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
        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(Category.builder()
                .id(id)
                .name("FOOD")
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        categoryService.deleteCategoryById(id, userId);

        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).deleteByIdAndUserId(id, userId);
    }

    @Test
    void deleteCategoryById_givenNonExistingId_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategoryById(id, userId));
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, never()).deleteByIdAndUserId(any(), any());
    }
}
