package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountAlreadyExistException;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
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
        return accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId())
                .map(this::reactivateAccount)
                .orElseGet(() -> createNewAccount(command));
    }

    private Account createNewAccount(CreateAccountCommand command) {
        Account account = Account.builder()
                .name(command.name())
                .active(true)
                .userId(command.userId())
                .currency(command.currency())
                .type(command.type())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account);
    }

    private Account reactivateAccount(Account existingAccount) {
        if (existingAccount.active()) {
            throw new AccountAlreadyExistException("Account with the same name and type already exists for the user");
        }
        return accountRepository.save(existingAccount.toBuilder()
                .active(true)
                .updatedAt(Instant.now())
                .build());
    }

    @Transactional
    @Override
    public Account patchAccount(PatchAccountCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Account existing = accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);

        String newName = command.name();
        Currency newCurrency = command.currency();
        AccountType newType = command.type();

        Account.AccountBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(newName)
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);
        Optional.ofNullable(newCurrency)
                .ifPresent(builder::currency);
        Optional.ofNullable(newType)
                .ifPresent(builder::type);

        return accountRepository.save(builder.build());
    }

    @Transactional
    @Override
    public void deleteAccountById(UUID id, UUID userId) {
        accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);

        accountRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void disableAccount(UUID id, UUID userId) {
        accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .ifPresentOrElse(
                        account -> accountRepository.save(account.toBuilder().active(false).build()),
                        () -> { throw new AccountNotFoundException(); }
                );
    }

    @Override
    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId) {
        return accountRepository.getAccountsByPageAndUserId(query, userId);
    }

    @Override
    public Page<AccountSummary> getAccountSummariesByPageQuery(PageQuery query, UUID userId, LocalDate monthStart, LocalDate monthEnd) {
        return accountRepository.findAllByPageQueryWithSummary(query, userId, monthStart, monthEnd);
    }

    @Transactional
    @Override
    public void deleteAllByUserId(UUID userId) {
        accountRepository.deleteAllByUserId(userId);
    }
}
