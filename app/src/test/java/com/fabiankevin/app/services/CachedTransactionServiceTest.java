//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.*;
//import com.fabiankevin.app.models.enums.SummaryType;
//import com.fabiankevin.app.models.enums.TransactionType;
//import com.fabiankevin.app.services.commands.AddTransactionCommand;
//import com.fabiankevin.app.services.commands.PatchTransactionCommand;
//import com.fabiankevin.app.services.queries.PageQuery;
//import com.fabiankevin.app.services.queries.SummaryQuery;
//import com.fabiankevin.app.web.controllers.dtos.AccountResponse;
//import com.fabiankevin.app.web.controllers.dtos.AmountResponse;
//import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
//import com.fabiankevin.app.web.controllers.dtos.TransactionResponse;
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
//import java.time.LocalDate;
//import java.util.Currency;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CachedTransactionServiceTest {
//
//    @Mock
//    private CacheManager cacheManager;
//    @Mock
//    private Cache cache;
//    @Mock
//    private TransactionService delegatedService;
//
//    private CachedTransactionService cachedTransactionService;
//
//    @BeforeEach
//    void setUp() {
//        when(cacheManager.getCache("transactions")).thenReturn(cache);
//        cachedTransactionService = new CachedTransactionService(cacheManager, delegatedService);
//    }
//
//    @Test
//    void getTransactionById_cacheMiss_delegatesAndCaches() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        TransactionResponse expected = TransactionResponse.builder()
//                .id(id)
//                .account(AccountResponse.builder().id(userId).name("GCASH").build())
//                .type(TransactionType.EXPENSE.name())
//                .category(CategoryResponse.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).build())
//                .amount(AmountResponse.builder().value(100.0).currency(Currency.getInstance("PHP")).build())
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getTransactionById(id, userId)).thenReturn(expected);
//
//        TransactionResponse result = cachedTransactionService.getTransactionById(id, userId);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getTransactionById(id, userId);
//        verify(cache, times(1)).put(eq(String.format("transactions:%s:byId:%s", userId, id)), eq(expected));
//    }
//
//    @Test
//    void getTransactionById_cacheHit_returnsCachedValue() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        TransactionResponse cached = TransactionResponse.builder()
//                .id(id)
//                .account(AccountResponse.builder().id(userId).name("GCASH").build())
//                .type(TransactionType.EXPENSE.name())
//                .category(CategoryResponse.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).build())
//                .amount(AmountResponse.builder().value(100.0).currency(Currency.getInstance("PHP")).build())
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        TransactionResponse result = cachedTransactionService.getTransactionById(id, userId);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getTransactionById(any(), any());
//    }
//
//    @Test
//    void getSummary_cacheMiss_delegatesAndCaches() {
//        UUID userId = UUID.randomUUID();
//        SummaryQuery query = SummaryQuery.builder()
//                .type(SummaryType.DAILY)
//                .from(LocalDate.of(2026, 7, 1))
//                .to(LocalDate.of(2026, 7, 7))
//                .userIds(Set.of(userId))
//                .transactionType(TransactionType.EXPENSE)
//                .build();
//        SummarySeries expected = new SummarySeries(SummaryType.DAILY, List.of(
//                new SummaryPoint("2026-07-01", 100L),
//                new SummaryPoint("2026-07-02", 200L)
//        ));
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getSummary(query)).thenReturn(expected);
//
//        SummarySeries result = cachedTransactionService.getSummary(query);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getSummary(query);
//        verify(cache, times(1)).put(any(String.class), eq(expected));
//    }
//
//    @Test
//    void getSummary_cacheHit_returnsCachedValue() {
//        UUID userId = UUID.randomUUID();
//        SummaryQuery query = SummaryQuery.builder()
//                .type(SummaryType.DAILY)
//                .from(LocalDate.of(2026, 7, 1))
//                .to(LocalDate.of(2026, 7, 7))
//                .userIds(Set.of(userId))
//                .transactionType(TransactionType.EXPENSE)
//                .build();
//        SummarySeries cached = new SummarySeries(SummaryType.DAILY, List.of(
//                new SummaryPoint("2026-07-01", 100L)
//        ));
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        SummarySeries result = cachedTransactionService.getSummary(query);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getSummary(any());
//    }
//
//    @Test
//    void getTransactionsByPageQuery_cacheMiss_delegatesAndCaches() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "transactionDate", "DESC");
//        Transaction tx = Transaction.builder()
//                .id(UUID.randomUUID())
//                .account(Account.builder().id(userId).name("GCASH").userId(userId).currency(Currency.getInstance("PHP")).build())
//                .type(TransactionType.EXPENSE)
//                .category(Category.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).userId(userId).build())
//                .amount(Amount.of(100.0, Currency.getInstance("PHP")))
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Transaction> expected = new Page<>(List.of(tx), 0, 10, 1L, 1, true, true);
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE)).thenReturn(expected);
//
//        Page<Transaction> result = cachedTransactionService.getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE);
//        verify(cache, times(1)).put(any(String.class), eq(expected));
//    }
//
//    @Test
//    void getTransactionsByPageQuery_cacheHit_returnsCachedValue() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "transactionDate", "DESC");
//        Transaction tx = Transaction.builder()
//                .id(UUID.randomUUID())
//                .account(Account.builder().id(userId).name("GCASH").userId(userId).currency(Currency.getInstance("PHP")).build())
//                .type(TransactionType.EXPENSE)
//                .category(Category.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).userId(userId).build())
//                .amount(Amount.of(100.0, Currency.getInstance("PHP")))
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Transaction> cached = new Page<>(List.of(tx), 0, 10, 1L, 1, true, true);
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        Page<Transaction> result = cachedTransactionService.getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getTransactionsByPageQuery(any(), any(), any());
//    }
//
//    @Test
//    void addTransaction_givenCacheKeysExist_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID transactionId = UUID.randomUUID();
//        AddTransactionCommand command = AddTransactionCommand.builder()
//                .amount(Amount.of(100.0, Currency.getInstance("PHP")))
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .categoryId(UUID.randomUUID())
//                .accountId(userId)
//                .userId(userId)
//                .build();
//        TransactionResponse cachedResponse = TransactionResponse.builder()
//                .id(transactionId)
//                .account(AccountResponse.builder().id(userId).name("GCASH").build())
//                .type(TransactionType.EXPENSE.name())
//                .category(CategoryResponse.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).build())
//                .amount(AmountResponse.builder().value(100.0).currency(Currency.getInstance("PHP")).build())
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        Transaction created = Transaction.builder()
//                .id(transactionId)
//                .account(Account.builder().id(userId).name("GCASH").userId(userId).currency(Currency.getInstance("PHP")).build())
//                .type(TransactionType.EXPENSE)
//                .category(Category.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).userId(userId).build())
//                .amount(Amount.of(100.0, Currency.getInstance("PHP")))
//                .description("Lunch")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        // Register keys via prior reads
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getTransactionById(eq(transactionId), eq(userId))).thenReturn(cachedResponse);
//        when(delegatedService.getTransactionsByPageQuery(any(), eq(userId), any())).thenReturn(
//                new Page<>(List.of(created), 0, 10, 1L, 1, true, true));
//        when(delegatedService.addTransaction(command)).thenReturn(created);
//
//        cachedTransactionService.getTransactionById(transactionId, userId);
//        cachedTransactionService.getTransactionsByPageQuery(new PageQuery(0, 10, "transactionDate", "DESC"), userId, TransactionType.EXPENSE);
//
//        // addTransaction should evict both registered keys
//        cachedTransactionService.addTransaction(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(2)).evict(evictCaptor.capture());
//        List<String> evictedKeys = evictCaptor.getAllValues();
//        assertEquals(2, evictedKeys.size());
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":byId:" + transactionId)));
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":paged:")));
//    }
//
//    @Test
//    void patchTransaction_givenCacheKeysExist_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID transactionId = UUID.randomUUID();
//        PatchTransactionCommand command = PatchTransactionCommand.builder()
//                .id(transactionId)
//                .description("Dinner")
//                .userId(userId)
//                .build();
//        Transaction patched = Transaction.builder()
//                .id(transactionId)
//                .account(Account.builder().id(userId).name("GCASH").userId(userId).currency(Currency.getInstance("PHP")).build())
//                .type(TransactionType.EXPENSE)
//                .category(Category.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).userId(userId).build())
//                .amount(Amount.of(150.0, Currency.getInstance("PHP")))
//                .description("Dinner")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        String key = "transactions:" + userId + ":byId:" + transactionId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getTransactionById(eq(transactionId), eq(userId))).thenReturn(
//                TransactionResponse.builder().id(transactionId).build());
//        when(delegatedService.patchTransaction(command)).thenReturn(patched);
//
//        // Register key via read
//        cachedTransactionService.getTransactionById(transactionId, userId);
//
//        // patchTransaction should evict the key
//        cachedTransactionService.patchTransaction(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        assertEquals(key, evictCaptor.getValue());
//    }
//
//    @Test
//    void deleteTransaction_givenCacheKeyExist_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID transactionId = UUID.randomUUID();
//
//        String key = "transactions:" + userId + ":byId:" + transactionId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getTransactionById(eq(transactionId), eq(userId))).thenReturn(
//                TransactionResponse.builder().id(transactionId).build());
//
//        // Register key via read
//        cachedTransactionService.getTransactionById(transactionId, userId);
//
//        // deleteTransaction should evict the key
//        cachedTransactionService.deleteTransaction(transactionId, userId);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        assertEquals(key, evictCaptor.getValue());
//    }
//
//    @Test
//    void evictUserKeys_noKeysRegistered_doesNothing() {
//        UUID userId = UUID.randomUUID();
//        lenient().when(cacheManager.getCache("transactions")).thenReturn(cache);
//
//        cachedTransactionService.deleteTransaction(UUID.randomUUID(), userId);
//
//        verify(cache, never()).evict(any());
//    }
//
//    @Test
//    void evictUserKeys_crossUserIsolation_doesNotAffectOtherUser() {
//        UUID userA = UUID.randomUUID();
//        UUID userB = UUID.randomUUID();
//        UUID transactionIdA = UUID.randomUUID();
//        UUID transactionIdB = UUID.randomUUID();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//
//        TransactionResponse responseA = TransactionResponse.builder()
//                .id(transactionIdA).build();
//        TransactionResponse responseB = TransactionResponse.builder()
//                .id(transactionIdB).build();
//
//        when(delegatedService.getTransactionById(eq(transactionIdA), eq(userA))).thenReturn(responseA);
//        when(delegatedService.getTransactionById(eq(transactionIdB), eq(userB))).thenReturn(responseB);
//
//        // Register keys for both users
//        cachedTransactionService.getTransactionById(transactionIdA, userA);
//        cachedTransactionService.getTransactionById(transactionIdB, userB);
//
//        // Evict user A's keys only — 1 eviction
//        cachedTransactionService.deleteTransaction(transactionIdA, userA);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        String evictedKeyA = evictCaptor.getValue();
//        assertTrue(evictedKeyA.contains(":byId:" + transactionIdA));
//        assertFalse(evictedKeyA.contains(String.valueOf(userB)));
//
//        // Register a paged key for user B
//        Transaction txB = Transaction.builder()
//                .id(UUID.randomUUID())
//                .account(Account.builder().id(userB).name("PAYMAY").userId(userB).currency(Currency.getInstance("PHP")).build())
//                .type(TransactionType.EXPENSE)
//                .category(Category.builder().id(UUID.randomUUID()).name("Food").type(TransactionType.EXPENSE).userId(userB).build())
//                .amount(Amount.of(100.0, Currency.getInstance("PHP")))
//                .description("Snack")
//                .transactionDate(LocalDate.now())
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Transaction> pagedResult = new Page<>(List.of(txB), 0, 10, 1L, 1, true, true);
//        when(delegatedService.getTransactionsByPageQuery(any(), eq(userB), any())).thenReturn(pagedResult);
//        cachedTransactionService.getTransactionsByPageQuery(new PageQuery(0, 10, "transactionDate", "DESC"), userB, TransactionType.EXPENSE);
//
//        // Now evict user B — should evict both byId and paged keys (2 more evictions)
//        cachedTransactionService.deleteTransaction(transactionIdB, userB);
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
