//package com.fabiankevin.app.services;
//
//import com.fabiankevin.app.models.Page;
//import com.fabiankevin.app.models.SummarySeries;
//import com.fabiankevin.app.models.Transaction;
//import com.fabiankevin.app.models.enums.TransactionType;
//import com.fabiankevin.app.services.commands.AddTransactionCommand;
//import com.fabiankevin.app.services.commands.PatchTransactionCommand;
//import com.fabiankevin.app.services.queries.PageQuery;
//import com.fabiankevin.app.services.queries.SummaryQuery;
//import com.fabiankevin.app.web.controllers.dtos.TransactionResponse;
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
//public class CachedTransactionService implements TransactionService {
//    private static final String CACHE_NAME = "transactions";
//    private static final String KEY_PREFIX = "transactions:";
//    private static final String KEY_BY_ID = KEY_PREFIX + "%s:byId:%s";
//    private static final String KEY_PAGED = KEY_PREFIX + "%s:paged:%d:%d:%s:%s";
//    private static final String KEY_SUMMARY = KEY_PREFIX + "%s:summary:%s:%s:%s:%s";
//
//    private final CacheManager cacheManager;
//    private final TransactionService delegatedTransactionService;
//
//    private final ConcurrentMap<UUID, CopyOnWriteArraySet<String>> userKeys = new ConcurrentHashMap<>();
//
//    @Override
//    public Transaction addTransaction(AddTransactionCommand command) {
//        Transaction result = delegatedTransactionService.addTransaction(command);
//        evictUserKeys(command.user());
//        return result;
//    }
//
//    @Override
//    public Transaction patchTransaction(PatchTransactionCommand command) {
//        Transaction result = delegatedTransactionService.patchTransaction(command);
//        evictUserKeys(command.user());
//        return result;
//    }
//
//    @Override
//    public void deleteTransaction(UUID transactionId, UUID user) {
//        delegatedTransactionService.deleteTransaction(transactionId, user);
//        evictUserKeys(user);
//    }
//
//    @Override
//    public TransactionResponse getTransactionById(UUID id, UUID user) {
//        String key = String.format(KEY_BY_ID, user, id);
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (TransactionResponse) cached.get();
//        }
//
//        TransactionResponse result = delegatedTransactionService.getTransactionById(id, user);
//        registerKey(user, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public SummarySeries getSummary(SummaryQuery query) {
//        String key = String.format(KEY_SUMMARY,
//                query.type(),
//                query.from() == null ? "null" : query.from(),
//                query.to() == null ? "null" : query.to(),
//                query.transactionType(),
//                query.userIds());
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (SummarySeries) cached.get();
//        }
//
//        for (UUID user : query.userIds()) {
//            registerKey(user, key);
//        }
//        SummarySeries result = delegatedTransactionService.getSummary(query);
//        cache.put(key, result);
//        return result;
//    }
//
//    @Override
//    public Page<Transaction> getTransactionsByPageQuery(PageQuery query, UUID user, TransactionType type) {
//        String key = String.format(KEY_PAGED, user, query.page(), query.size(),
//                query.sort(), query.direction(), type);
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//
//        Cache.ValueWrapper cached = cache.get(key);
//        if (cached != null) {
//            return (Page<Transaction>) cached.get();
//        }
//
//        Page<Transaction> result = delegatedTransactionService.getTransactionsByPageQuery(query, user, type);
//        registerKey(user, key);
//        cache.put(key, result);
//        return result;
//    }
//
//    private void registerKey(UUID user, String key) {
//        userKeys.computeIfAbsent(user, _ -> new CopyOnWriteArraySet<>()).add(key);
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
