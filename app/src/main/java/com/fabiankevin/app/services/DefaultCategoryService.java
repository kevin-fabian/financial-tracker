package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryAlreadyExistException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
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
        Category category = Category.of(command.name(), command.type(), command.userId(), command.iconName());
        if (categoryRepository.existsByNameAndTypeAndUserId(command.name(), command.type(), command.userId())) {
            throw new CategoryAlreadyExistException("Category with the same name and type already exists for the user");
        }

        return categoryRepository.save(category);
    }

    @Transactional
    @Override
    public void deleteCategoryById(UUID id, UUID userId) {
        categoryRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public Page<Category> getCategoriesByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        return categoryRepository.findAllByPageQuery(query, userId, type);
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

        Optional.ofNullable(command.iconName())
                .ifPresent(categoryBuilder::iconName);

        return categoryRepository.save(categoryBuilder.build());
    }
}
