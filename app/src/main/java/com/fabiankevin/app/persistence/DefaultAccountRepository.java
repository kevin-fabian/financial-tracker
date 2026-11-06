package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
    private final EntityManager entityManager;

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaAccountRepository.findById(id)
                .map(AccountEntity::toModel);
    }

    @Transactional
    @Override
    public Account save(Account account) {
        if (account == null) {
            throw new InvalidDataAccessApiUsageException("Account must not be null");
        }
        AccountEntity entity = AccountEntity.from(account);
        entity.setId(Optional.ofNullable(entity.getId()).orElse(UUID.randomUUID()));
        return entityManager.merge(entity).toModel();
    }

    @Override
    public Account update(Account account) {
        if (account == null) {
            throw new InvalidDataAccessApiUsageException("Account must not be null");
        }
        AccountEntity entity = AccountEntity.from(account);
        return entityManager.merge(entity).toModel();
    }

    @Transactional
    @Override
    public List<Account> saveAll(List<Account> accounts) {
        List<AccountEntity> entities = accounts.stream()
                .map(AccountEntity::from)
                .map(entity -> {
                    entity.setId(Optional.ofNullable(entity.getId()).orElse(UUID.randomUUID()));
                    return entityManager.merge(entity);
                })
                .toList();

        return entities.stream()
                .map(AccountEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaAccountRepository.deleteById(id);
    }

    @Override
    public Page<AccountSummary> findAllByPageQueryWithSummary(PageQuery query, List<UUID> userIds, LocalDate monthStart, LocalDate monthEnd) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );
        var entityPage = jpaAccountRepository.findAllByUserIdsWithSummary(userIds, monthStart, monthEnd, pageable);

        List<AccountSummary> content = entityPage.getContent().stream()
                .map(projection -> AccountSummary.builder()
                        .account(projection.account().toModel())
                        .totalBalance(projection.totalBalance())
                        .totalTransactions(projection.totalTransactions())
                        .localId(projection.account().getLocalId())
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
                        .localId(projection.account().getLocalId())
                        .build());
    }

    @Override
    public Optional<Account> findByNameAndTypeAndUserId(String name, AccountType type, UUID userId) {
        return jpaAccountRepository.findByNameAndTypeAndUserId(name, type.name(), userId)
                .map(AccountEntity::toModel);
    }

    @Override
    public List<Account> findByNameAndTypeAndUserIdBatch(List<CreateAccountCommand> commands, UUID userId) {
        List<String> names = commands.stream()
                .map(CreateAccountCommand::name)
                .distinct()
                .toList();
        List<String> types = commands.stream()
                .map(cmd -> cmd.type().name())
                .distinct()
                .toList();

        return jpaAccountRepository.findAllByNamesAndTypesAndUserId(names, types, userId).stream()
                .map(AccountEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findAllByUserId(UUID userId) {
        return jpaAccountRepository.findAllByUserId(userId).stream()
                .map(AccountEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public long deleteAllByUserId(UUID userId) {
        return jpaAccountRepository.deleteAllByUserId(userId);
    }

}
