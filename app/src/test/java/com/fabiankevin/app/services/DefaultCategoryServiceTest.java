package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.IconRepository;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private IconRepository iconRepository;

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
    void createCategory_givenExistingIcon_shouldResolveAndSaveCategory() {
        UUID userId = UUID.randomUUID();
        IconData existingIcon = IconData.builder()
                .id(UUID.randomUUID())
                .codePoint(0x1F370)
                .fontFamily("MaterialIcons")
                .iconName("restaurant")
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(IconData.builder()
                        .id(existingIcon.id())
                        .codePoint(0x1F370)
                        .fontFamily("MaterialIcons")
                        .iconName("restaurant")
                        .build())
                .build();

        when(iconRepository.findByCodePointAndFontFamily(0x1F370, "MaterialIcons"))
                .thenReturn(Optional.of(existingIcon));
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("FOOD", created.name());
        assertEquals(TransactionType.EXPENSE, created.type());
        assertEquals(existingIcon, created.icon());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(0x1F370, "MaterialIcons");
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_givenNewIcon_shouldBuildAndSaveCategory() {
        UUID userId = UUID.randomUUID();
        IconData newIcon = IconData.builder()
                .codePoint(0x1F3E0)
                .fontFamily("MaterialIcons")
                .iconName("home")
                .build();

        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.empty());
        when(iconRepository.save(any())).thenAnswer(invocation -> (IconData) invocation.getArguments()[0]);

        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });

        Category created = categoryService.createCategory(command);

        assertEquals("RENT", created.name());
        assertEquals(TransactionType.EXPENSE, created.type());
        assertEquals(newIcon.codePoint(), created.icon().codePoint());
        assertEquals(newIcon.fontFamily(), created.icon().fontFamily());
        assertEquals(newIcon.iconName(), created.icon().iconName());
        assertNull(created.icon().id());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(0x1F3E0, "MaterialIcons");
        ArgumentCaptor<Category> categoryArgumentCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(categoryArgumentCaptor.capture());
        Category value = categoryArgumentCaptor.getValue();
        assertNull(value.id(), "id");
        assertEquals(value.name(), created.name(), "name");
        assertEquals(value.type(), created.type(), "type");
        assertEquals(value.userId(), created.userId(), "userId");
        assertNull(value.icon().id(), "icon.id");
        assertEquals(newIcon.codePoint(), value.icon().codePoint(), "codePoint");
        assertEquals(newIcon.fontFamily(), value.icon().fontFamily(), "fontFamily");
        assertNotNull(value.icon().createdAt(), "createdAt");
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
    void patchCategory_givenNewIcon_shouldUpdateCategoryIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID existingIconId = UUID.randomUUID();

        IconData existingIcon = IconData.builder()
                .id(existingIconId)
                .codePoint(0x1F370)
                .fontFamily("MaterialIcons")
                .iconName("restaurant")
                .build();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.empty());
        when(iconRepository.save(any())).thenAnswer(invocation -> (IconData) invocation.getArguments()[0]);

        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(existingIcon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IconData newIcon = IconData.builder()
                .codePoint(0x1F3E0)
                .fontFamily("MaterialIcons")
                .iconName("home")
                .build();

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
        assertNotNull(updated.icon(), "icon");
        assertNull(updated.icon().id(), "icon.id should be null (new icon not yet persisted)");
        assertEquals(newIcon.codePoint(), updated.icon().codePoint(), "icon.codePoint");
        assertEquals(newIcon.fontFamily(), updated.icon().fontFamily(), "icon.fontFamily");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).save(any());
        verify(iconRepository, times(1)).save(any());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(newIcon.codePoint(), "MaterialIcons");
    }

    @Test
    void patchCategory_givenExistingIcon_shouldUpdateCategoryIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IconData existingIcon = IconData.builder()
                .id(UUID.randomUUID())
                .codePoint(0x1F370)
                .fontFamily("MaterialIcons")
                .iconName("restaurant")
                .build();

        Category existing = Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(existingIcon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IconData newIcon = IconData.builder()
                .codePoint(0x1F3E0)
                .fontFamily("MaterialIcons")
                .iconName("home")
                .build();

        PatchCategoryCommand command = PatchCategoryCommand.builder()
                .id(id)
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.of(newIcon.toBuilder().id(UUID.randomUUID()).build()));

        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndTypeAndUserId("RENT", TransactionType.EXPENSE, userId)).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.patchCategory(command);

        IconData updatedIcon = updated.icon();

        assertEquals("RENT", updated.name());
        assertNotNull(updatedIcon, "icon");
        assertNotNull(updatedIcon.id(), "icon.id");
        assertEquals(updatedIcon.fontFamily(), "MaterialIcons", "icon.fontFamily");
        assertEquals(updatedIcon.codePoint(), 0x1F3E0, "icon.codePoint");
        verify(categoryRepository, times(1)).findByIdAndUserId(id, userId);
        verify(categoryRepository, times(1)).save(any());
    }
}
