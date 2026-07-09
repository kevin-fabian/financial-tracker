package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserAccountProvider {
    List<Account> provide(Set<String> accountInterests, UUID userId);
}
