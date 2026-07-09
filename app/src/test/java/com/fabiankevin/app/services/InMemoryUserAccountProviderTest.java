package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryUserAccountProviderTest {

    @Mock
    private AccountService accountService;

    private InMemoryUserAccountProvider provider;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provider = new InMemoryUserAccountProvider(accountService);
        testUserId = UUID.randomUUID();
    }

    @Nested
    class Provide {

        @Test
        void provide_nullInterests_doesNotCallService() {
            provider.provide(null, testUserId);
            verify(accountService, never()).createAccount(any());
        }

        @Test
        void provide_emptyInterests_doesNotCallService() {
            provider.provide(Set.of(), testUserId);
            verify(accountService, never()).createAccount(any());
        }

        @Test
        void provide_nullUserId_throwsIllegalArgumentException() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> provider.provide(Set.of("gcash"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provide_knownInterests_callsServiceWithCorrectCommands() {
            Set<String> interests = Set.of("gcash", "maya");

            provider.provide(interests, testUserId);

            verify(accountService, times(2)).createAccount(any(CreateAccountCommand.class));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("Maya")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provide_unknownInterests_doesNotCallService() {
            Set<String> interests = Set.of("unknown_interest");

            provider.provide(interests, testUserId);

            verify(accountService, never()).createAccount(any());
        }

        @Test
        void provide_mixedInterests_callsServiceOnlyForKnown() {
            Set<String> interests = Set.of("gcash", "unknown", "bank");

            provider.provide(interests, testUserId);

            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("Bank Account")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provide_allInterests_callsServiceForAllAccounts() {
            Set<String> interests = Set.of("gcash", "maya", "bank", "credit_card");

            provider.provide(interests, testUserId);

            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("Maya")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("Bank Account")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
            verify(accountService).createAccount(eq(CreateAccountCommand.builder()
                    .name("Credit Card")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .userId(testUserId)
                    .build()));
        }
    }
}
