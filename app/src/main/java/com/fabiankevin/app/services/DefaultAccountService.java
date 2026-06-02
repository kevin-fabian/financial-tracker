package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.AccountType;
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
        Optional<IconData> optionalIconData = getOrSaveIcon(command.icon());

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
        Optional.ofNullable(command.icon())
                .ifPresent(iconData -> {
                    if(existing.icon() != null && iconData.id() == existing.icon().id() ){
                        return;
                    }
                    builder.icon(getOrSaveIcon(iconData).orElse(null));
                });

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

    private Optional<IconData> getOrSaveIcon(IconData icon) {
        if (icon == null) {
            return Optional.empty();
        }

        return Optional.of(iconRepository.findByCodePointAndFontFamily(icon.codePoint(), icon.fontFamily())
                .orElseGet(() -> {
                    IconData newIcon = IconData.builder()
                            .codePoint(icon.codePoint())
                            .fontFamily(icon.fontFamily())
                            .iconName(icon.iconName())
                            .createdAt(Instant.now())
                            .build();

                    return iconRepository.save(newIcon);
                }));
    }
}
