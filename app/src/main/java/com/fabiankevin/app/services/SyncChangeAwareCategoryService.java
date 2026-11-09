package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeAwareCategoryService implements CategoryService {
    private final CategoryService delegate;
    private final SyncChangeService syncChangeService;
    private final JsonMapper jsonMapper;

    public SyncChangeAwareCategoryService(
            @Qualifier("defaultCategoryService")
            CategoryService delegate, SyncChangeService syncChangeService, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.syncChangeService = syncChangeService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public CategorySummary getCategoryById(final UUID id, final UUID userId) {
        return delegate.getCategoryById(id, userId);
    }

    @Override
    public Category createCategory(final CreateCategoryCommand command) {
        final Category result = delegate.createCategory(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.CATEGORIES)
                .entityId(result.id())
                .operation(SyncChangeOperationType.CREATE)
                .payload(jsonMapper.writeValueAsString(result))
                .build());
        return result;
    }

    @Override
    public void createCategories(final List<CreateCategoryCommand> commands, final UUID userId) {
        delegate.createCategories(commands, userId);
    }

    @Override
    public CategorySummary patchCategory(final PatchCategoryCommand command) {
        final CategorySummary result = delegate.patchCategory(command);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(command.userId())
                .entityType(ResourceType.CATEGORIES)
                .entityId(result.category().id())
                .operation(SyncChangeOperationType.UPDATE)
                .payload(jsonMapper.writeValueAsString(result))
                .build());
        return result;
    }

    @Override
    public void deleteCategoryById(final UUID id, final UUID userId) {
        delegate.deleteCategoryById(id, userId);
    }

    @Override
    public void disableCategory(final UUID id, final UUID userId) {
        final CategorySummary existing = delegate.getCategoryById(id, userId);
        delegate.disableCategory(id, userId);
        syncChangeService.create(CreateSyncChangeCommand.builder()
                .userId(userId)
                .entityType(ResourceType.CATEGORIES)
                .entityId(id)
                .operation(SyncChangeOperationType.DELETE)
                .payload(jsonMapper.writeValueAsString(existing))
                .build());
    }

    @Override
    public Page<Category> getCategoriesByPageQuery(final PageQuery query, final UUID userId,
            final TransactionType type) {
        return delegate.getCategoriesByPageQuery(query, userId, type);
    }

    @Override
    public Page<CategorySummary> getCategorySummariesByPageQuery(final PageQuery query, final UUID userId,
            final TransactionType type) {
        return delegate.getCategorySummariesByPageQuery(query, userId, type);
    }

    @Override
    public void deleteAllByUserId(final UUID userId) {
        delegate.deleteAllByUserId(userId);
    }
}
