package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@RequiredArgsConstructor
@Primary
public class CachedCategoryService implements CategoryService {
    private static final String CACHE_NAME = "categories";
    private static final String KEY_PREFIX = "categories:";
    private static final String KEY_BY_ID = KEY_PREFIX + "%s:byId:%s";
    private static final String KEY_PAGED = KEY_PREFIX + "%s:paged:%d:%d:%s:%s:%s";

    private final CacheManager cacheManager;
    private final CategoryService delegatedCategoryService;

    private final ConcurrentMap<UUID, CopyOnWriteArraySet<String>> userKeys = new ConcurrentHashMap<>();

    @Override
    public Category getCategoryById(UUID id, UUID userId) {
        String key = String.format(KEY_BY_ID, userId, id);
        Cache cache = cacheManager.getCache(CACHE_NAME);

        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            return (Category) cached.get();
        }

        Category result = delegatedCategoryService.getCategoryById(id, userId);
        registerKey(userId, key);
        cache.put(key, result);
        return result;
    }

    @Override
    public Category createCategory(CreateCategoryCommand command) {
        Category result = delegatedCategoryService.createCategory(command);
        evictUserKeys(command.userId());
        return result;
    }

    @Override
    public Category patchCategory(PatchCategoryCommand command) {
        Category result = delegatedCategoryService.patchCategory(command);
        evictUserKeys(command.userId());
        return result;
    }

    @Override
    public void deleteCategoryById(UUID id, UUID userId) {
        delegatedCategoryService.deleteCategoryById(id, userId);
        evictUserKeys(userId);
    }

    @Override
    public void disableCategory(UUID id, UUID userId) {
        delegatedCategoryService.disableCategory(id, userId);
        evictUserKeys(userId);
    }

    @Override
    public Page<Category> getCategoriesByPageQuery(PageQuery query, UUID userId, TransactionType type) {
        String key = String.format(KEY_PAGED, userId, query.page(), query.size(),
                query.sort(), query.direction(), type);
        Cache cache = cacheManager.getCache(CACHE_NAME);

        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            return (Page<Category>) cached.get();
        }

        Page<Category> result = delegatedCategoryService.getCategoriesByPageQuery(query, userId, type);
        registerKey(userId, key);
        cache.put(key, result);
        return result;
    }

    private void registerKey(UUID userId, String key) {
        userKeys.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(key);
    }

    private void evictUserKeys(UUID userId) {
        CopyOnWriteArraySet<String> keys = userKeys.remove(userId);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        for (String key : keys) {
            cache.evict(key);
        }
    }
}
