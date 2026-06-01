package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.IconRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DefaultAccountService implements AccountService {
    private final AccountRepository accountRepository;
    private final IconRepository iconRepository;

    @Override
    public Account getAccountById(UUID id, UUID userId) {
        return accountRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .orElseThrow(AccountNotFoundException::new);
    }

    @Transactional
    @Override
    public Account createAccount(CreateAccountCommand command) {
        Optional<IconData> optionalIconData = getIconData(command);

        Account account = Account.builder()
                .name(command.name())
                .userId(command.userId())
                .icon(optionalIconData.orElse(null))
                .currency(command.currency())
                .type(command.type())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account);
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
        com.fabiankevin.app.models.enums.AccountType newType = command.type();

        com.fabiankevin.app.models.IconData newIcon = command.icon();

        Account.AccountBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(newName)
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);
        Optional.ofNullable(newCurrency)
                .ifPresent(builder::currency);
        Optional.ofNullable(newType)
                .ifPresent(builder::type);
        Optional.ofNullable(newIcon)
                .ifPresent(builder::icon);

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

    @Override
    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId) {
        return accountRepository.getAccountsByPageAndUserId(query, userId);
    }

    private Optional<IconData> getIconData(CreateAccountCommand command) {
        if (command.icon() == null) {
            return Optional.empty();
        }

        IconData icon = command.icon();
        return Optional.of(iconRepository.findByCodePointAndFontFamily(icon.codePoint(), icon.fontFamily())
                .orElse(IconData.builder()
                        .codePoint(icon.codePoint())
                        .fontFamily(icon.fontFamily())
                        .iconName(icon.iconName())
                        .createdAt(Instant.now())
                        .build()));
    }
}
