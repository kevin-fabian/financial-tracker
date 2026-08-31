package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class DefaultAccountRepository implements AccountRepository {
    private final JpaAccountRepository jpaAccountRepository;

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaAccountRepository.findById(id)
                .map(AccountEntity::toModel);
    }

    @Override
    public Account save(Account account) {
        AccountEntity saved = jpaAccountRepository.save(AccountEntity.from(account));
        return saved.toModel();
    }

    @Override
    public void deleteById(UUID id) {
        jpaAccountRepository.deleteById(id);
    }

    @Override
    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );

        var page = jpaAccountRepository.findAllByUserId(userId, pageable)
                .map(AccountEntity::toModel);

        return new Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    @Override
    public Page<AccountSummary> findAllByPageQueryWithSummary(PageQuery query, UUID userId, LocalDate monthStart, LocalDate monthEnd) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );
        var entityPage = jpaAccountRepository.findAllByUserIdWithSummary(userId, monthStart, monthEnd, pageable);

        List<AccountSummary> content = entityPage.getContent().stream()
                .map(projection -> AccountSummary.builder()
                        .account(projection.account().toModel())
                        .totalBalance(projection.totalBalance())
                        .totalTransactions(projection.totalTransactions())
                        .build())
                .collect(Collectors.toList());

        return new Page<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Override
    public Optional<AccountSummary> findSummaryByIdAndUserId(UUID accountId, UUID userId) {
        return jpaAccountRepository.findSummaryByIdAndUserId(accountId, userId)
                .map(projection -> AccountSummary.builder()
                        .account(projection.account().toModel())
                        .totalBalance(projection.totalBalance())
                        .totalTransactions(projection.totalTransactions())
                        .build());
    }

    @Override
    public Optional<Account> findByNameAndTypeAndUserId(String name, AccountType type, UUID userId) {
        return jpaAccountRepository.findByNameAndTypeAndUserId(name, type.name(), userId)
                .map(AccountEntity::toModel);
    }

    @Override
    public List<Account> findAllByNamesIn(List<String> accountNames) {
        return jpaAccountRepository.findAllByNameIn(accountNames)
                .stream()
                .map(AccountEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public long deleteAllByUserId(UUID userId) {
        return jpaAccountRepository.deleteAllByUserId(userId);
    }

}
