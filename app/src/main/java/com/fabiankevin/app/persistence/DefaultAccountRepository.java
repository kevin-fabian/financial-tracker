package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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

}
