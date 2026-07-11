//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.Account;
//import com.fabiankevin.app.models.AccountSummary;
//import com.fabiankevin.app.models.Page;
//import com.fabiankevin.app.services.commands.CreateAccountCommand;
//import com.fabiankevin.app.services.commands.PatchAccountCommand;
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
//public class CachedAccountService implements AccountService {
//    private static final String CACHE_NAME = "accounts";
//    private static final String KEY_PREFIX = "accounts:";
//    private static final String KEY_BY_ID = KEY_PREFIX + "%s:byId:%s";
//    private static final String KEY_PAGED = KEY_PREFIX + "%s:paged:%d:%d:%s:%s";
//
//    private final CacheManager cacheManager;
//    private final AccountService delegatedAccountService;
//
//    private final ConcurrentMap<UUID, CopyOnWriteArraySet<String>> userKeys = new ConcurrentHashMap<>();
//
//    @Override
//    public Account getAccountById(UUID id, UUID id) {
//        String key = String.format(KEY_BY_ID, id, id);
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Account) cached.get();
//        }
//
//        Account result = delegatedAccountService.getAccountById(id, id);
//        registerKey(id, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public Account createAccount(CreateAccountCommand command) {
//        Account result = delegatedAccountService.createAccount(command);
//        evictUserKeys(command.id());
//        return result;
//    }
//
//    @Override
//    public Account patchAccount(PatchAccountCommand command) {
//        Account result = delegatedAccountService.patchAccount(command);
//        evictUserKeys(command.id());
//        return result;
//    }
//
//    @Override
//    public void deleteAccountById(UUID id, UUID id) {
//        delegatedAccountService.deleteAccountById(id, id);
//        evictUserKeys(id);
//    }
//
//    @Override
//    public void disableAccount(UUID id, UUID id) {
//        delegatedAccountService.disableAccount(id, id);
//        evictUserKeys(id);
//    }
//
//    @Override
//    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID id) {
//        String key = String.format(KEY_PAGED, id, query.page(), query.size(),
//                query.sort(), query.direction());
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Page<Account>) cached.get();
//        }
//
//        Page<Account> result = delegatedAccountService.getAccountsByPageAndUserId(query, id);
//        registerKey(id, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public Page<AccountSummary> getAccountSummariesByPageQuery(PageQuery query, UUID id) {
//        String key = String.format(KEY_PAGED, id, query.page(), query.size(),
//                query.sort(), query.direction());
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Page<AccountSummary>) cached.get();
//        }
//
//        Page<AccountSummary> result = delegatedAccountService.getAccountSummariesByPageQuery(query, id);
//        registerKey(id, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    private void registerKey(UUID id, String key) {
//        userKeys.computeIfAbsent(id, k -> new CopyOnWriteArraySet<>()).add(key);
//    }
//
//    private void evictUserKeys(UUID id) {
//        CopyOnWriteArraySet<String> keys = userKeys.remove(id);
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
