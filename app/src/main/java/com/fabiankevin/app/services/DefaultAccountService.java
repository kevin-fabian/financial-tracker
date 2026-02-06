package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DefaultAccountService implements AccountService {
    private final AccountRepository accountRepository;

    @Override
    public Account getAccountById(UUID id, UUID userId) {
        return accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);
    }

    @Transactional
    @Override
    public Account createAccount(CreateAccountCommand command) {
        Account account = Account.builder()
                .name(command.name())
                .userId(command.userId())
                .currency(command.currency())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account);
    }

    @Transactional
    @Override
    public void deleteAccountById(UUID id, UUID userId) {
        accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);

        accountRepository.deleteById(id);
    }

    @Override
    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId) {
        return accountRepository.getAccountsByPageAndUserId(query, userId);
    }
}
