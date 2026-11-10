package com.fabiankevin.app.categories.domain.services;

import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.categories.domain.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategorySummary getCategoryById(UUID id, UUID userId);
    Category createCategory(CreateCategoryCommand command);
    void createCategories(List<CreateCategoryCommand> commands, UUID userId);
    CategorySummary patchCategory(PatchCategoryCommand command);
    void deleteCategoryById(UUID id, UUID userId);
    void disableCategory(UUID id, UUID userId);
    Page<Category> getCategoriesByPageQuery(PageQuery query, UUID userId, TransactionType type);
    Page<CategorySummary> getCategorySummariesByPageQuery(PageQuery query, UUID userId, TransactionType type);
    void deleteAllByUserId(UUID userId);
}
