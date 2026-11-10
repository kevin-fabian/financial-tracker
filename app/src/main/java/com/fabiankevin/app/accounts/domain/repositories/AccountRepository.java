package com.fabiankevin.app.accounts.domain.repositories;

import com.fabiankevin.app.accounts.domain.models.Account;
import com.fabiankevin.app.accounts.domain.models.AccountSummary;
import com.fabiankevin.app.accounts.domain.models.AccountType;
import com.fabiankevin.app.accounts.domain.services.commands.CreateAccountCommand;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.queries.PageQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    Account save(Account account);

    Account update(Account account);

    List<Account> saveAll(List<Account> accounts);

    void deleteById(UUID id);

    Page<AccountSummary> findAllByPageQueryWithSummary(PageQuery query, List<UUID> userIds, LocalDate monthStart, LocalDate monthEnd);

    Optional<AccountSummary> findSummaryByIdAndUserId(UUID accountId, UUID userId);

    Optional<Account> findByNameAndTypeAndUserId(String name, AccountType type, UUID userId);

    List<Account> findByNameAndTypeAndUserIdBatch(List<CreateAccountCommand> commands, UUID userId);

    List<Account> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
