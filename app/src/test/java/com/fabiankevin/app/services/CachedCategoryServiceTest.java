//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.Category;
//import com.fabiankevin.app.models.Page;
//import com.fabiankevin.app.models.enums.TransactionType;
//import com.fabiankevin.app.services.commands.CreateCategoryCommand;
//import com.fabiankevin.app.services.commands.PatchCategoryCommand;
//import com.fabiankevin.app.services.queries.PageQuery;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CachedCategoryServiceTest {
//
//    @Mock
//    private CacheManager cacheManager;
//    @Mock
//    private Cache cache;
//    @Mock
//    private CategoryService delegatedService;
//
//    private CachedCategoryService cachedCategoryService;
//
//    @BeforeEach
//    void setUp() {
//        when(cacheManager.getCache("categories")).thenReturn(cache);
//        cachedCategoryService = new CachedCategoryService(cacheManager, delegatedService);
//    }
//
//    @Test
//    void getCategoryById_cacheMiss_delegatesAndCaches() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        Category expected = Category.builder()
//                .id(id)
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getCategoryById(id, userId)).thenReturn(expected);
//
//        Category result = cachedCategoryService.getCategoryById(id, userId);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getCategoryById(id, userId);
//        verify(cache, times(1)).put(eq(String.format("categories:%s:byId:%s", userId, id)), eq(expected));
//    }
//
//    @Test
//    void getCategoryById_cacheHit_returnsCachedValue() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        Category cached = Category.builder()
//                .id(id)
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        Category result = cachedCategoryService.getCategoryById(id, userId);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getCategoryById(any(), any());
//    }
//
//    @Test
//    void getCategoriesByPageQuery_cacheMiss_delegatesAndCaches() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "name", "ASC");
//        TransactionType type = TransactionType.EXPENSE;
//        Category c1 = Category.builder()
//                .id(UUID.randomUUID())
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Category> expected = new Page<>(List.of(c1), 0, 10, 1L, 1, true, true);
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getCategoriesByPageQuery(query, userId, type)).thenReturn(expected);
//
//        Page<Category> result = cachedCategoryService.getCategoriesByPageQuery(query, userId, type);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getCategoriesByPageQuery(query, userId, type);
//        verify(cache, times(1)).put(any(String.class), eq(expected));
//    }
//
//    @Test
//    void getCategoriesByPageQuery_cacheHit_returnsCachedValue() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "name", "ASC");
//        TransactionType type = TransactionType.EXPENSE;
//        Category c1 = Category.builder()
//                .id(UUID.randomUUID())
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Category> cached = new Page<>(List.of(c1), 0, 10, 1L, 1, true, true);
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        Page<Category> result = cachedCategoryService.getCategoriesByPageQuery(query, userId, type);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getCategoriesByPageQuery(any(), any(), any());
//    }
//
//    @Test
//    void createCategory_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID categoryId = UUID.randomUUID();
//        CreateCategoryCommand command = CreateCategoryCommand.builder()
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .build();
//        Category created = Category.builder()
//                .id(categoryId)
//                .name("FOOD")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        // Pre-populate cache with keys for this user
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getCategoryById(eq(categoryId), eq(userId))).thenReturn(created);
//        when(delegatedService.getCategoriesByPageQuery(any(), eq(userId), any())).thenReturn(
//                new Page<>(List.of(created), 0, 10, 1L, 1, true, true));
//        when(delegatedService.createCategory(command)).thenReturn(created);
//
//        // Register keys via reads
//        cachedCategoryService.getCategoryById(categoryId, userId);
//        cachedCategoryService.getCategoriesByPageQuery(new PageQuery(0, 10, "name", "ASC"), userId, TransactionType.EXPENSE);
//
//        // Now create — should evict both registered keys
//        cachedCategoryService.createCategory(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(2)).evict(evictCaptor.capture());
//        List<String> evictedKeys = evictCaptor.getAllValues();
//        // Should contain the byId key and the paged key for this user
//        assertEquals(2, evictedKeys.size());
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":byId:" + categoryId)));
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":paged:")));
//    }
//
//    @Test
//    void patchCategory_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID categoryId = UUID.randomUUID();
//        PatchCategoryCommand command = PatchCategoryCommand.builder()
//                .id(categoryId)
//                .name("GROCERIES")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .build();
//        Category patched = Category.builder()
//                .id(categoryId)
//                .name("GROCERIES")
//                .type(TransactionType.EXPENSE)
//                .userId(userId)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        String key = "categories:" + userId + ":byId:" + categoryId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getCategoryById(eq(categoryId), eq(userId))).thenReturn(patched);
//        when(delegatedService.patchCategory(command)).thenReturn(patched);
//
//        // Register key via read
//        cachedCategoryService.getCategoryById(categoryId, userId);
//
//        // Patch — should evict the key
//        cachedCategoryService.patchCategory(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        assertEquals(key, evictCaptor.getValue());
//    }
//
//    @Test
//    void deleteCategoryById_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID categoryId = UUID.randomUUID();
//
//        String key = "categories:" + userId + ":byId:" + categoryId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getCategoryById(eq(categoryId), eq(userId))).thenReturn(
//                Category.builder().id(categoryId).name("FOOD").type(TransactionType.EXPENSE).userId(userId)
//                        .createdAt(Instant.now()).updatedAt(Instant.now()).build());
//
//        // Register key via read
//        cachedCategoryService.getCategoryById(categoryId, userId);
//
//        // Delete — should evict the key
//        cachedCategoryService.deleteCategoryById(categoryId, userId);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        assertEquals(key, evictCaptor.getValue());
//    }
//
//    @Test
//    void evictUserKeys_noKeysRegistered_doesNothing() {
//        UUID userId = UUID.randomUUID();
//        // Use lenient stubbing since this test doesn't interact with the cache
//        lenient().when(cacheManager.getCache("categories")).thenReturn(cache);
//
//        cachedCategoryService.deleteCategoryById(UUID.randomUUID(), userId);
//
//        verify(cache, never()).evict(any());
//    }
//
//    @Test
//    void evictUserKeys_crossUserIsolation_doesNotAffectOtherUser() {
//        UUID userA = UUID.randomUUID();
//        UUID userB = UUID.randomUUID();
//        UUID categoryIdA = UUID.randomUUID();
//        UUID categoryIdB = UUID.randomUUID();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//
//        Category catA = Category.builder()
//                .id(categoryIdA).name("FOOD").type(TransactionType.EXPENSE).userId(userA)
//                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
//        Category catB = Category.builder()
//                .id(categoryIdB).name("RENT").type(TransactionType.EXPENSE).userId(userB)
//                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
//
//        when(delegatedService.getCategoryById(eq(categoryIdA), eq(userA))).thenReturn(catA);
//        when(delegatedService.getCategoryById(eq(categoryIdB), eq(userB))).thenReturn(catB);
//
//        // Register keys for both users
//        cachedCategoryService.getCategoryById(categoryIdA, userA);
//        cachedCategoryService.getCategoryById(categoryIdB, userB);
//
//        // Evict user A's keys only — 1 eviction
//        cachedCategoryService.deleteCategoryById(categoryIdA, userA);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        String evictedKeyA = evictCaptor.getValue();
//        assertTrue(evictedKeyA.contains(":byId:" + categoryIdA));
//        assertFalse(evictedKeyA.contains(String.valueOf(userB)));
//
//        // Register a paged key for user B
//        Page<Category> pagedResult = new Page<>(List.of(catB), 0, 10, 1L, 1, true, true);
//        when(delegatedService.getCategoriesByPageQuery(any(), eq(userB), any())).thenReturn(pagedResult);
//        cachedCategoryService.getCategoriesByPageQuery(new PageQuery(0, 10, "name", "ASC"), userB, TransactionType.EXPENSE);
//
//        // Now evict user B — should evict both byId and paged keys (2 more evictions)
//        cachedCategoryService.deleteCategoryById(categoryIdB, userB);
//
//        verify(cache, times(3)).evict(any()); // 1 for A + 2 for B
//    }
//
//    private Cache.ValueWrapper mockValueWrapper(Object value) {
//        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
//        when(wrapper.get()).thenReturn(value);
//        return wrapper;
//    }
//}
