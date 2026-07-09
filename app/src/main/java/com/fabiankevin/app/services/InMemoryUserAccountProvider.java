package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class InMemoryUserAccountProvider implements UserAccountProvider {
    private final AccountService accountService;

    @Override
    public void provide(Set<String> accountInterests, UUID userId) {
        if (accountInterests == null || accountInterests.isEmpty()) {
            return;
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        accountInterests.stream()
                .filter(ACCOUNT_INTERESTS_MAPPING::containsKey)
                .flatMap(interest -> ACCOUNT_INTERESTS_MAPPING.get(interest).stream())
                .forEach(command -> accountService.createAccount(command.toBuilder().userId(userId).build()));
    }

    private static final Map<String, List<CreateAccountCommand>> ACCOUNT_INTERESTS_MAPPING = Map.ofEntries(
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
