package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;

import java.util.UUID;

public interface CategoryService {
    Category getCategoryById(UUID id, UUID userId);
    Category createCategory(CreateCategoryCommand command);
    void deleteCategoryById(UUID id, UUID userId);
}
