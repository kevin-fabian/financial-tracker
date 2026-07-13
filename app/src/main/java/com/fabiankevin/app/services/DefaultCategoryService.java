package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryAlreadyExistException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultCategoryService implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public Category getCategoryById(UUID id, UUID userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    @Transactional
    @Override
    public Category createCategory(CreateCategoryCommand command) {
        return categoryRepository.findInactiveByNameAndTypeAndUserId(command.name(), command.type(), command.userId())
                .map(inactiveCategory -> reactivateCategory(inactiveCategory, command))
                .orElseGet(() -> createNewCategory(command));
    }

    private Category createNewCategory(CreateCategoryCommand command) {
        if (categoryRepository.existsByNameAndTypeAndUserId(command.name(), command.type(), command.userId())) {
            throw new CategoryAlreadyExistException("Category with the same name and type already exists for the user");
        }

        Category newCategory = Category.builder()
                .name(command.name())
                .type(command.type())
                .icon(command.icon())
                .userId(command.userId())
                .active(true)
                .system(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return categoryRepository.save(newCategory);
    }

    private Category reactivateCategory(Category inactiveCategory, CreateCategoryCommand command) {
        return categoryRepository.save(inactiveCategory.toBuilder()
                .active(true)
                .icon(command.icon())
                .updatedAt(Instant.now())
                .build());
    }

    @Transactional
    @Override
    public void deleteCategoryById(UUID id, UUID userId) {
        categoryRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    @Override
    public void disableCategory(UUID id, UUID userId) {
        categoryRepository.findByIdAndUserId(id, userId)
                .ifPresentOrElse(
                        category -> categoryRepository.save(category.toBuilder().active(false).build()),
                        () -> { throw new CategoryNotFoundException(); }
                );
    }

    @Override
    public Page<Category> getCategoriesByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        return categoryRepository.findAllByPageQuery(query, userId, type);
    }

    @Override
    public Page<CategorySummary> getCategorySummariesByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        return categoryRepository.findAllByPageQueryWithSummary(query, userId, type);
    }

    @Transactional
    @Override
    public Category patchCategory(PatchCategoryCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Category existing = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(CategoryNotFoundException::new);

        String newName = command.name();
        TransactionType newType = command.type();
        if (newName != null && !newName.isBlank() && !newName.equals(existing.name()) && categoryRepository.existsByNameAndTypeAndUserId(newName, existing.type(), userId)) {
            throw new CategoryAlreadyExistException("Category with the same name and type already exists for the user");
        }

        Category.CategoryBuilder categoryBuilder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(newName)
                .filter(name -> !name.isBlank())
                .ifPresent(categoryBuilder::name);

        Optional.ofNullable(newType)
                .ifPresent(categoryBuilder::type);

        Optional.ofNullable(command.icon())
                .ifPresent(categoryBuilder::icon);

        return categoryRepository.save(categoryBuilder.build());
    }

    @Transactional
    @Override
    public void deleteAllByUserId(UUID userId) {
        categoryRepository.deleteAllByUserId(userId);
    }
}
