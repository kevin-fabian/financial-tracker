package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.enums.AccountType;

import java.time.Instant;
import java.util.*;

public class InMemoryUserAccountProvider implements UserAccountProvider {
    @Override
    public List<Account> provide(Set<String> accountInterests, UUID userId) {
        if (accountInterests == null || accountInterests.isEmpty()) {
            return Collections.emptyList();
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return accountInterests.stream()
                .filter(ACCOUNT_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> ACCOUNT_INTERESTS_MAPPING.get(interest).stream())
                .map(account -> account.withUserId(userId))
                .toList();
    }

    private static final Map<String, List<Account>> ACCOUNT_INTERESTS_MAPPING = Map.ofEntries(
            Map.entry("gcash", List.of(
                    Account.builder().name("GCash").currency(Currency.getInstance("PHP")).userId(null).type(AccountType.E_WALLET).active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("maya", List.of(
                    Account.builder().name("Maya").currency(Currency.getInstance("PHP")).userId(null).type(AccountType.E_WALLET).active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("bank", List.of(
                    Account.builder().name("Bank Account").currency(Currency.getInstance("PHP")).userId(null).type(AccountType.E_WALLET).active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            )),
            Map.entry("credit_card", List.of(
                    Account.builder().name("Credit Card").currency(Currency.getInstance("PHP")).userId(null).type(AccountType.E_WALLET).active(true).system(false).createdAt(Instant.now()).updatedAt(Instant.now()).build()
            ))
    );
}
