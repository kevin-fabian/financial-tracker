package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.services.queries.PageQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    Account save(Account account);

    void deleteById(UUID id);

    Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId);

    Page<AccountSummary> findAllByPageQueryWithSummary(PageQuery query, List<UUID> userIds, LocalDate monthStart, LocalDate monthEnd);

    Optional<AccountSummary> findSummaryByIdAndUserId(UUID accountId, UUID userId);

    List<Account> findAllByNamesIn(List<String> accountNames);

    Optional<Account> findByNameAndTypeAndUserId(String name, AccountType type, UUID userId);

    long deleteAllByUserId(UUID userId);
}
