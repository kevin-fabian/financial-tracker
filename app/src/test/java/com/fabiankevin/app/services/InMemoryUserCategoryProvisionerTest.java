package com.fabiankevin.app.services;

import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static com.fabiankevin.app.models.enums.TransactionType.INCOME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryUserCategoryProvisionerTest {

    @Mock
    private CategoryService categoryService;

    private InMemoryUserCategoryProvisioner provider;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provider = new InMemoryUserCategoryProvisioner(categoryService);
        testUserId = UUID.randomUUID();
    }

    @Nested
    class Provide {

        @Test
        void provide_nullInterests_doesNotCallService() {
            provider.provision(null, testUserId);
            verify(categoryService, never()).createCategory(any());
        }

        @Test
        void provide_emptyInterests_doesNotCallService() {
            provider.provision(Set.of(), testUserId);
            verify(categoryService, never()).createCategory(any());
        }

        @Test
        void provide_nullUserId_throwsIllegalArgumentException() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> provider.provision(Set.of("groceries"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provide_knownInterests_callsServiceWithCorrectCommands() {
            Set<String> interests = Set.of("groceries", "bills");

            provider.provision(interests, testUserId);

            verify(categoryService, times(3)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Groceries")
                    .type(EXPENSE)
                    .icon("local_grocery_store")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Utilities")
                    .type(EXPENSE)
                    .icon("bolt")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Subscriptions")
                    .type(EXPENSE)
                    .icon("card_membership")
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provide_unknownInterests_doesNotCallService() {
            Set<String> interests = Set.of("unknown_category");

            provider.provision(interests, testUserId);

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        void provide_mixedInterests_callsServiceOnlyForKnown() {
            Set<String> interests = Set.of("groceries", "unknown", "rent");

            provider.provision(interests, testUserId);

            verify(categoryService, times(3)).createCategory(any(CreateCategoryCommand.class));
        }

        @Test
        void provide_incomeInterests_callsServiceWithIncomeCategories() {
            Set<String> interests = Set.of("salary_active", "passive_investments");

            provider.provision(interests, testUserId);

            verify(categoryService, times(2)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Salary")
                    .type(INCOME)
                    .icon("payments")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Investments")
                    .type(INCOME)
                    .icon("trending_up")
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provide_multiCategoryInterests_callsServiceForAllSubCategories() {
            Set<String> interests = Set.of("shopping", "health_fitness");

            provider.provision(interests, testUserId);

            verify(categoryService, times(4)).createCategory(any(CreateCategoryCommand.class));
        }

        @Test
        void provide_allInterests_callsServiceForAllCategories() {
            Set<String> interests = Set.of(
                    "groceries", "bills", "rent", "entertainment", "savings",
                    "shopping", "health_fitness", "family_pets", "debt_loans",
                    "salary_active", "business_sales", "passive_investments", "allowances_gifts"
            );

            provider.provision(interests, testUserId);

            verify(categoryService, times(19)).createCategory(any(CreateCategoryCommand.class));
        }
    }
}
