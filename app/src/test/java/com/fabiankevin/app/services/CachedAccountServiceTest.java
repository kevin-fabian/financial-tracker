//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.Account;
//import com.fabiankevin.app.models.Page;
//import com.fabiankevin.app.models.enums.AccountType;
//import com.fabiankevin.app.services.commands.CreateAccountCommand;
//import com.fabiankevin.app.services.commands.PatchAccountCommand;
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
//import java.util.Currency;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CachedAccountServiceTest {
//
//    @Mock
//    private CacheManager cacheManager;
//    @Mock
//    private Cache cache;
//    @Mock
//    private AccountService delegatedService;
//
//    private CachedAccountService cachedAccountService;
//
//    @BeforeEach
//    void setUp() {
//        when(cacheManager.getCache("accounts")).thenReturn(cache);
//        cachedAccountService = new CachedAccountService(cacheManager, delegatedService);
//    }
//
//    @Test
//    void getAccountById_cacheMiss_delegatesAndCaches() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        Account expected = Account.builder()
//                .id(id)
//                .name("GCASH")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getAccountById(id, userId)).thenReturn(expected);
//
//        Account result = cachedAccountService.getAccountById(id, userId);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getAccountById(id, userId);
//        verify(cache, times(1)).put(eq(String.format("accounts:%s:byId:%s", userId, id)), eq(expected));
//    }
//
//    @Test
//    void getAccountById_cacheHit_returnsCachedValue() {
//        UUID id = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        Account cached = Account.builder()
//                .id(id)
//                .name("GCASH")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        Account result = cachedAccountService.getAccountById(id, userId);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getAccountById(any(), any());
//    }
//
//    @Test
//    void getAccountsByPageAndUserId_cacheMiss_delegatesAndCaches() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "name", "ASC");
//        Account a1 = Account.builder()
//                .id(UUID.randomUUID())
//                .name("GCASH")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Account> expected = new Page<>(List.of(a1), 0, 10, 1L, 1, true, true);
//
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getAccountsByPageAndUserId(query, userId)).thenReturn(expected);
//
//        Page<Account> result = cachedAccountService.getAccountsByPageAndUserId(query, userId);
//
//        assertSame(expected, result);
//        verify(delegatedService, times(1)).getAccountsByPageAndUserId(query, userId);
//        verify(cache, times(1)).put(any(String.class), eq(expected));
//    }
//
//    @Test
//    void getAccountsByPageAndUserId_cacheHit_returnsCachedValue() {
//        UUID userId = UUID.randomUUID();
//        PageQuery query = new PageQuery(0, 10, "name", "ASC");
//        Account a1 = Account.builder()
//                .id(UUID.randomUUID())
//                .name("GCASH")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//        Page<Account> cached = new Page<>(List.of(a1), 0, 10, 1L, 1, true, true);
//        Cache.ValueWrapper wrapper = mockValueWrapper(cached);
//        when(cache.get(any(String.class))).thenReturn(wrapper);
//
//        Page<Account> result = cachedAccountService.getAccountsByPageAndUserId(query, userId);
//
//        assertSame(cached, result);
//        verify(delegatedService, never()).getAccountsByPageAndUserId(any(), any());
//    }
//
//    @Test
//    void createAccount_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID accountId = UUID.randomUUID();
//        CreateAccountCommand command = CreateAccountCommand.builder()
//                .name("GCASH")
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .userId(userId)
//                .build();
//        Account created = Account.builder()
//                .id(accountId)
//                .name("GCASH")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        // Pre-populate cache with keys for this user
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getAccountById(eq(accountId), eq(userId))).thenReturn(created);
//        when(delegatedService.getAccountsByPageAndUserId(any(), eq(userId))).thenReturn(
//                new Page<>(List.of(created), 0, 10, 1L, 1, true, true));
//        when(delegatedService.createAccount(command)).thenReturn(created);
//
//        // Register keys via reads
//        cachedAccountService.getAccountById(accountId, userId);
//        cachedAccountService.getAccountsByPageAndUserId(new PageQuery(0, 10, "name", "ASC"), userId);
//
//        // Now create — should evict both registered keys
//        cachedAccountService.createAccount(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(2)).evict(evictCaptor.capture());
//        List<String> evictedKeys = evictCaptor.getAllValues();
//        // Should contain the byId key and the paged key for this user
//        assertEquals(2, evictedKeys.size());
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":byId:" + accountId)));
//        assertTrue(evictedKeys.stream().anyMatch(k -> k.contains(":paged:")));
//    }
//
//    @Test
//    void patchAccount_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID accountId = UUID.randomUUID();
//        PatchAccountCommand command = PatchAccountCommand.builder()
//                .id(accountId)
//                .name("GCASH_MAIN")
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .userId(userId)
//                .build();
//        Account patched = Account.builder()
//                .id(accountId)
//                .name("GCASH_MAIN")
//                .userId(userId)
//                .currency(Currency.getInstance("PHP"))
//                .type(AccountType.E_WALLET)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
//                .build();
//
//        String key = "accounts:" + userId + ":byId:" + accountId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getAccountById(eq(accountId), eq(userId))).thenReturn(patched);
//        when(delegatedService.patchAccount(command)).thenReturn(patched);
//
//        // Register key via read
//        cachedAccountService.getAccountById(accountId, userId);
//
//        // Patch — should evict the key
//        cachedAccountService.patchAccount(command);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        assertEquals(key, evictCaptor.getValue());
//    }
//
//    @Test
//    void deleteAccountById_evictsUserKeys() {
//        UUID userId = UUID.randomUUID();
//        UUID accountId = UUID.randomUUID();
//
//        String key = "accounts:" + userId + ":byId:" + accountId;
//        when(cache.get(any(String.class))).thenReturn(null);
//        when(delegatedService.getAccountById(eq(accountId), eq(userId))).thenReturn(
//                Account.builder().id(accountId).name("GCASH").userId(userId)
//                        .currency(Currency.getInstance("PHP")).type(AccountType.E_WALLET)
//                        .createdAt(Instant.now()).updatedAt(Instant.now()).build());
//
//        // Register key via read
//        cachedAccountService.getAccountById(accountId, userId);
//
//        // Delete — should evict the key
//        cachedAccountService.deleteAccountById(accountId, userId);
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
//        lenient().when(cacheManager.getCache("accounts")).thenReturn(cache);
//
//        cachedAccountService.deleteAccountById(UUID.randomUUID(), userId);
//
//        verify(cache, never()).evict(any());
//    }
//
//    @Test
//    void evictUserKeys_crossUserIsolation_doesNotAffectOtherUser() {
//        UUID userA = UUID.randomUUID();
//        UUID userB = UUID.randomUUID();
//        UUID accountIdA = UUID.randomUUID();
//        UUID accountIdB = UUID.randomUUID();
//
//        when(cache.get(any(String.class))).thenReturn(null);
//
//        Account accountA = Account.builder()
//                .id(accountIdA).name("GCASH").userId(userA)
//                .currency(Currency.getInstance("PHP")).type(AccountType.E_WALLET)
//                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
//        Account accountB = Account.builder()
//                .id(accountIdB).name("PAYMAY").userId(userB)
//                .currency(Currency.getInstance("PHP")).type(AccountType.E_WALLET)
//                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
//
//        when(delegatedService.getAccountById(eq(accountIdA), eq(userA))).thenReturn(accountA);
//        when(delegatedService.getAccountById(eq(accountIdB), eq(userB))).thenReturn(accountB);
//
//        // Register keys for both users
//        cachedAccountService.getAccountById(accountIdA, userA);
//        cachedAccountService.getAccountById(accountIdB, userB);
//
//        // Evict user A's keys only — 1 eviction
//        cachedAccountService.deleteAccountById(accountIdA, userA);
//
//        ArgumentCaptor<String> evictCaptor = ArgumentCaptor.forClass(String.class);
//        verify(cache, times(1)).evict(evictCaptor.capture());
//        String evictedKeyA = evictCaptor.getValue();
//        assertTrue(evictedKeyA.contains(":byId:" + accountIdA));
//        assertFalse(evictedKeyA.contains(String.valueOf(userB)));
//
//        // Register a paged key for user B
//        Page<Account> pagedResult = new Page<>(List.of(accountB), 0, 10, 1L, 1, true, true);
//        when(delegatedService.getAccountsByPageAndUserId(any(), eq(userB))).thenReturn(pagedResult);
//        cachedAccountService.getAccountsByPageAndUserId(new PageQuery(0, 10, "name", "ASC"), userB);
//
//        // Now evict user B — should evict both byId and paged keys (2 more evictions)
//        cachedAccountService.deleteAccountById(accountIdB, userB);
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
