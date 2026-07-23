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
class InMemoryUserAccountProvisionerTest {

    @Mock
    private AccountService accountService;

    private InMemoryUserAccountProvisioner provider;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provider = new InMemoryUserAccountProvisioner(accountService);
        testUserId = UUID.randomUUID();
    }

    @Nested
    class Provision {

        @Test
        void provide_nullInterests_doesNotCallService() {
            provider.provision(null, testUserId);
            verify(accountService, never()).createAccount(any());
        }

        @Test
        void provide_emptyInterests_doesNotCallService() {
            provider.provision(Set.of(), testUserId);
            verify(accountService, never()).createAccount(any());
        }

        @Test
        void provide_nullUserId_throwsIllegalArgumentException() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> provider.provision(Set.of("gcash"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provide_knownInterests_callsServiceWithCorrectCommands() {
            Set<String> interests = Set.of("gcash", "maya");

            provider.provision(interests, testUserId);

            verify(accountService, times(3)).createAccount(any(CreateAccountCommand.class));
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
        void provide_unknownInterests_shouldProvisionDefaultAccounts() {
            Set<String> interests = Set.of("unknown_interest");

            provider.provision(interests, testUserId);

            verify(accountService, times(1))
                    .createAccount(argThat(acc -> "Cash Wallet".equalsIgnoreCase(acc.name())));
        }

        @Test
        void provide_mixedInterests_callsServiceOnlyForKnown() {
            Set<String> interests = Set.of("gcash", "unknown", "bank");

            provider.provision(interests, testUserId);

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

            provider.provision(interests, testUserId);

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

            verify(accountService, times(1)).deleteAllByUserId(testUserId);
        }
    }
}
