package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.enums.AccountType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserAccountProviderTest {

    private InMemoryUserAccountProvider provider;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provider = new InMemoryUserAccountProvider();
        testUserId = UUID.randomUUID();
    }

    @Nested
    class Provide {

        @Test
        void provide_nullInterests_returnsEmptyList() {
            assertThat(provider.provide(null, testUserId))
                    .as("Result should be empty list for null interests")
                    .isEmpty();
        }

        @Test
        void provide_emptyInterests_returnsEmptyList() {
            assertThat(provider.provide(Set.of(), testUserId))
                    .as("Result should be empty list for empty interests")
                    .isEmpty();
        }

        @Test
        void provide_nullUserId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> provider.provide(Set.of("gcash"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provide_knownInterests_returnsMappedAccounts() {
            Set<String> interests = Set.of("gcash", "maya");

            List<Account> accounts = provider.provide(interests, testUserId);

            assertThat(accounts)
                    .as("Should return 2 accounts for 2 known interests")
                    .hasSize(2);
            assertThat(accounts)
                    .extracting(Account::name)
                    .containsExactlyInAnyOrder("GCash", "Maya");
            assertThat(accounts)
                    .allSatisfy(account -> {
                        assertThat(account.userId()).isEqualTo(testUserId);
                        assertThat(account.currency()).isEqualTo(Currency.getInstance("PHP"));
                        assertThat(account.type()).isEqualTo(AccountType.E_WALLET);
                    });
        }

        @Test
        void provide_unknownInterests_returnsEmptyList() {
            Set<String> interests = Set.of("unknown_interest");

            List<Account> accounts = provider.provide(interests, testUserId);

            assertThat(accounts)
                    .as("Should return empty list for unknown interests")
                    .isEmpty();
        }

        @Test
        void provide_mixedInterests_returnsOnlyKnownAccounts() {
            Set<String> interests = Set.of("gcash", "unknown", "bank");

            List<Account> accounts = provider.provide(interests, testUserId);

            assertThat(accounts)
                    .as("Should return accounts only for known interests")
                    .hasSize(2);
            assertThat(accounts)
                    .extracting(Account::name)
                    .containsExactlyInAnyOrder("GCash", "Bank Account");
        }

        @Test
        void provide_allInterests_returnsAllAccounts() {
            Set<String> interests = Set.of("gcash", "maya", "bank", "credit_card");

            List<Account> accounts = provider.provide(interests, testUserId);

            assertThat(accounts)
                    .as("Should return all 4 accounts for all known interests")
                    .hasSize(4);
            assertThat(accounts)
                    .extracting(Account::name)
                    .containsExactlyInAnyOrder("GCash", "Maya", "Bank Account", "Credit Card");
        }
    }
}
