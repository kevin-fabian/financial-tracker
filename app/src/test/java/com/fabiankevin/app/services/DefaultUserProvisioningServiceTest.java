package com.fabiankevin.app.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultUserProvisioningServiceTest {

    @Mock
    private UserAccountProvisioner accountProvider;

    @Mock
    private UserCategoryProvisioner categoryProvider;

    private DefaultUserProvisioningService service;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        service = new DefaultUserProvisioningService(accountProvider, categoryProvider);
        testUserId = UUID.randomUUID();
    }

    @Nested
    class ProvisionUser {

        @Test
        void provisionUser_givenValidInterests_callsBothProviders() {
            Set<String> accountInterests = Set.of("gcash", "bank");
            Set<String> categoryInterests = Set.of("groceries", "salary_active");

            service.provisionUser(testUserId, accountInterests, categoryInterests);

            verify(accountProvider, times(1)).provision(accountInterests, testUserId);
            verify(categoryProvider, times(1)).provision(categoryInterests, testUserId);
        }

        @Test
        void provisionUser_givenEmptyInterests_callsBothProvidersWithEmptySets() {
            service.provisionUser(testUserId, Set.of(), Set.of());

            verify(accountProvider, times(1)).provision(Set.of(), testUserId);
            verify(categoryProvider, times(1)).provision(Set.of(), testUserId);
        }

        @Test
        void provisionUser_givenNullAccountInterests_callsAccountProviderWithNull() {
            Set<String> categoryInterests = Set.of("groceries");

            service.provisionUser(testUserId, null, categoryInterests);

            verify(accountProvider, times(1)).provision(null, testUserId);
            verify(categoryProvider, times(1)).provision(categoryInterests, testUserId);
        }

        @Test
        void provisionUser_givenNullCategoryInterests_callsCategoryProviderWithNull() {
            Set<String> accountInterests = Set.of("gcash");

            service.provisionUser(testUserId, accountInterests, null);

            verify(accountProvider, times(1)).provision(accountInterests, testUserId);
            verify(categoryProvider, times(1)).provision(null, testUserId);
        }

        @Test
        void provisionUser_givenNullBothInterests_callsBothProvidersWithNull() {
            service.provisionUser(testUserId, null, null);

            verify(accountProvider, times(1)).provision(null, testUserId);
            verify(categoryProvider, times(1)).provision(null, testUserId);
        }

        @Test
        void provisionUser_givenNullUserId_throwsIllegalArgumentException() {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> service.provisionUser(null, Set.of("gcash"), Set.of("groceries")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void provisionUser_givenAllAccountInterests_callsAccountProviderWithAll() {
            Set<String> accountInterests = Set.of("gcash", "maya", "bank", "credit_card");
            Set<String> categoryInterests = Set.of(
                    "groceries", "bills", "rent", "entertainment", "savings",
                    "shopping", "health_fitness", "family_pets", "debt_loans",
                    "salary_active", "business_sales", "passive_investments", "allowances_gifts"
            );

            service.provisionUser(testUserId, accountInterests, categoryInterests);

            verify(accountProvider, times(1)).provision(accountInterests, testUserId);
            verify(categoryProvider, times(1)).provision(categoryInterests, testUserId);
        }
    }
}
