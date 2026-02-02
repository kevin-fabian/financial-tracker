package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryAlreadyExistException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
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
        Category category = Category.of(command.name(), command.userId());
        if (categoryRepository.existsByNameAndUserId(command.name(), command.userId())) {
            throw new CategoryAlreadyExistException("Category with the same name already exists for the user");
        }

        return categoryRepository.save(category);
    }

    @Transactional
    @Override
    public void deleteCategoryById(UUID id, UUID userId) {
        categoryRepository.findByIdAndUserId(id, userId).orElseThrow(CategoryNotFoundException::new);
        categoryRepository.deleteByIdAndUserId(id, userId);
    }
}
