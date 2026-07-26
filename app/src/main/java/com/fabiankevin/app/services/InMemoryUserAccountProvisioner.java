package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class InMemoryUserAccountProvisioner implements UserAccountProvisioner {
    private final AccountService accountService;

    @Transactional
    @Override
    public void provision(Set<String> accountInterests, UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Set<String> accountInterestsWithDefault = new HashSet<>(Optional.ofNullable(accountInterests).orElse(Set.of()));
        accountInterestsWithDefault.add("default");

        accountService.deleteAllByUserId(userId);
        accountInterestsWithDefault.stream()
                .filter(ACCOUNT_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> ACCOUNT_INTERESTS_MAPPING.get(interest).stream())
                .forEach(command -> accountService.createAccount(command.toBuilder().userId(userId).build()));
    }

    private static final Map<String, List<CreateAccountCommand>> ACCOUNT_INTERESTS_MAPPING = Map.ofEntries(
            Map.entry("default", List.of(CreateAccountCommand.builder()
                    .name("Cash Wallet")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.CASH)
                    .build())),
            Map.entry("gcash", List.of(CreateAccountCommand.builder()
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .build())),
            Map.entry("maya", List.of(CreateAccountCommand.builder()
                    .name("Maya")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .build())),
            Map.entry("bank", List.of(CreateAccountCommand.builder()
                    .name("Bank Account")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .build())),
            Map.entry("credit_card", List.of(CreateAccountCommand.builder()
                    .name("Credit Card")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .build()))
    );
}
