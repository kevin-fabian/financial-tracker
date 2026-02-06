package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.UUID;

public interface CategoryService {
    Category getCategoryById(UUID id, UUID userId);
    Category createCategory(CreateCategoryCommand command);
    Category patchCategory(PatchCategoryCommand command);
    void deleteCategoryById(UUID id, UUID userId);
    Page<Category> getCategoriesByPageQuery(PageQuery query, UUID userId);
}
