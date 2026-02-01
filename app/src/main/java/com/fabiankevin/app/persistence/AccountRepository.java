package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
}
