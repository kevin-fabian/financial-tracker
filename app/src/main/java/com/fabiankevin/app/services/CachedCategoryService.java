//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.Category;
//import com.fabiankevin.app.models.CategorySummary;
//import com.fabiankevin.app.models.Page;
//import com.fabiankevin.app.models.enums.TransactionType;
//import com.fabiankevin.app.services.commands.CreateCategoryCommand;
//import com.fabiankevin.app.services.commands.PatchCategoryCommand;
//import com.fabiankevin.app.services.queries.PageQuery;
//import lombok.RequiredArgsConstructor;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.context.annotation.Primary;
//
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.CopyOnWriteArraySet;
//
//@RequiredArgsConstructor
//@Primary
//public class CachedCategoryService implements CategoryService {
//    private static final String CACHE_NAME = "categories";
//    private static final String KEY_PREFIX = "categories:";
//    private static final String KEY_BY_ID = KEY_PREFIX + "%s:byId:%s";
//    private static final String KEY_PAGED = KEY_PREFIX + "%s:paged:%d:%d:%s:%s:%s";
//
//    private final CacheManager cacheManager;
//    private final CategoryService delegatedCategoryService;
//
//    private final ConcurrentMap<UUID, CopyOnWriteArraySet<String>> userKeys = new ConcurrentHashMap<>();
//
//    @Override
//    public Category getCategoryById(UUID id, UUID user) {
//        String key = String.format(KEY_BY_ID, user, id);
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Category) cached.get();
//        }
//
//        Category result = delegatedCategoryService.getCategoryById(id, user);
//        registerKey(user, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public Category createCategory(CreateCategoryCommand command) {
//        Category result = delegatedCategoryService.createCategory(command);
//        evictUserKeys(command.user());
//        return result;
//    }
//
//    @Override
//    public Category patchCategory(PatchCategoryCommand command) {
//        Category result = delegatedCategoryService.patchCategory(command);
//        evictUserKeys(command.user());
//        return result;
//    }
//
//    @Override
//    public void deleteCategoryById(UUID id, UUID user) {
//        delegatedCategoryService.deleteCategoryById(id, user);
//        evictUserKeys(user);
//    }
//
//    @Override
//    public void disableCategory(UUID id, UUID user) {
//        delegatedCategoryService.disableCategory(id, user);
//        evictUserKeys(user);
//    }
//
//    @Override
//    public Page<Category> getCategoriesByPageQuery(PageQuery query, UUID user, TransactionType type) {
//        String key = String.format(KEY_PAGED, user, query.page(), query.size(),
//                query.sort(), query.direction(), type);
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Page<Category>) cached.get();
//        }
//
//        Page<Category> result = delegatedCategoryService.getCategoriesByPageQuery(query, user, type);
//        registerKey(user, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public Page<CategorySummary> getCategorySummariesByPageQuery(PageQuery query, UUID user, TransactionType type) {
//        String key = String.format(KEY_PAGED, user, query.page(), query.size(),
//                query.sort(), query.direction(), type) + ":summary";
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Page<CategorySummary>) cached.get();
//        }
//
//        Page<CategorySummary> result = delegatedCategoryService.getCategorySummariesByPageQuery(query, user, type);
//        registerKey(user, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public void deleteAllByUserId(UUID user) {
//        evictUserKeys(user);
//        delegatedCategoryService.deleteAllByUserId(user);
//    }
//
//    private void registerKey(UUID user, String key) {
//        userKeys.computeIfAbsent(user, k -> new CopyOnWriteArraySet<>()).add(key);
//    }
//
//    private void evictUserKeys(UUID user) {
//        CopyOnWriteArraySet<String> keys = userKeys.remove(user);
//        if (keys == null || keys.isEmpty()) {
//            return;
//        }
//
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        for (String key : keys) {
//            cache.evict(key);
//        }
//    }
//}
