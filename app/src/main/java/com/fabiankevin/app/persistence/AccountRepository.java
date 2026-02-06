package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    Account save(Account account);

    void deleteById(UUID id);

    Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId);
}
