package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserCategoryProviderTest {

    private InMemoryUserCategoryProvider provider;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provider = new InMemoryUserCategoryProvider();
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
            assertThatThrownBy(() -> provider.provide(Set.of("groceries"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provide_knownInterests_returnsMappedCategories() {
            Set<String> interests = Set.of("groceries", "bills");

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return 3 categories (1 groceries + 2 bills)")
                    .hasSize(3);
            assertThat(categories)
                    .extracting(Category::name)
                    .containsExactlyInAnyOrder("Groceries", "Utilities", "Subscriptions");
            assertThat(categories)
                    .allSatisfy(category -> {
                        assertThat(category.userId()).isEqualTo(testUserId);
                        assertThat(category.type()).isEqualTo(TransactionType.EXPENSE);
                        assertThat(category.active()).isTrue();
                        assertThat(category.system()).isFalse();
                    });
        }

        @Test
        void provide_unknownInterests_returnsEmptyList() {
            Set<String> interests = Set.of("unknown_category");

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return empty list for unknown interests")
                    .isEmpty();
        }

        @Test
        void provide_mixedInterests_returnsOnlyKnownCategories() {
            Set<String> interests = Set.of("groceries", "unknown", "rent");

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return categories only for known interests")
                    .hasSize(3);
            assertThat(categories)
                    .extracting(Category::name)
                    .containsExactlyInAnyOrder("Groceries", "Housing & Rent", "Home Maintenance");
        }

        @Test
        void provide_incomeInterests_returnsIncomeCategories() {
            Set<String> interests = Set.of("salary_active", "passive_investments");

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return 2 income categories")
                    .hasSize(2);
            assertThat(categories)
                    .extracting(Category::name)
                    .containsExactlyInAnyOrder("Salary", "Investments");
            assertThat(categories)
                    .allSatisfy(category -> assertThat(category.type()).isEqualTo(TransactionType.INCOME));
        }

        @Test
        void provide_multiCategoryInterests_returnsAllSubCategories() {
            Set<String> interests = Set.of("shopping", "health_fitness");

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return 4 categories (2 shopping + 2 health_fitness)")
                    .hasSize(4);
            assertThat(categories)
                    .extracting(Category::name)
                    .containsExactlyInAnyOrder("Shopping", "Personal Care", "Healthcare", "Fitness & Wellness");
        }

        @Test
        void provide_allInterests_returnsAllCategories() {
            Set<String> interests = Set.of(
                    "groceries", "bills", "rent", "entertainment", "savings",
                    "shopping", "health_fitness", "family_pets", "debt_loans",
                    "salary_active", "business_sales", "passive_investments", "allowances_gifts"
            );

            List<Category> categories = provider.provide(interests, testUserId);

            assertThat(categories)
                    .as("Should return all 19 categories for all known interests")
                    .hasSize(19);
        }
    }
}
