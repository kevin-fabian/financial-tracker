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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InMemoryUserCategoryProvisionerTest {

    @Mock
    private CategoryService categoryService;

    private InMemoryUserCategoryProvisioner provisioner;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        provisioner = new InMemoryUserCategoryProvisioner(categoryService);
        testUserId = UUID.randomUUID();
    }

    @Nested
    class Provision {

        @Test
        void provision_nullInterests_provisionsDefaultCategories() {
            provisioner.provision(null, testUserId);

            verify(categoryService, times(3)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Food & Dining")
                    .type(EXPENSE)
                    .icon("restaurant")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Transportation")
                    .type(EXPENSE)
                    .icon("directions_bus")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Side Hustle")
                    .type(INCOME)
                    .icon("attach_money")
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provision_emptyInterests_provisionsDefaultCategories() {
            provisioner.provision(Set.of(), testUserId);

            verify(categoryService, times(3)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Food & Dining")
                    .type(EXPENSE)
                    .icon("restaurant")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Transportation")
                    .type(EXPENSE)
                    .icon("directions_bus")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Side Hustle")
                    .type(INCOME)
                    .icon("attach_money")
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provision_nullUserId_throwsIllegalArgumentException() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> provisioner.provision(Set.of("groceries"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be null");
        }

        @Test
        void provision_knownInterests_callsServiceWithCorrectCommandsAndDefaults() {
            Set<String> interests = Set.of("groceries", "bills");

            provisioner.provision(interests, testUserId);

            // default (3) + groceries (1) + bills (2) = 6
            verify(categoryService, times(6)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Groceries")
                    .type(EXPENSE)
                    .icon("local_grocery_store")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Utilities & Bills")
                    .type(EXPENSE)
                    .icon("receipt_long")
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
        void provision_unknownInterests_createsDefaultCategories() {
            Set<String> interests = Set.of("unknown_category");

            provisioner.provision(interests, testUserId);

            // default (3) categories are always created
            verify(categoryService, times(3)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Food & Dining")
                    .type(EXPENSE)
                    .icon("restaurant")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Transportation")
                    .type(EXPENSE)
                    .icon("directions_bus")
                    .userId(testUserId)
                    .build()));
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Side Hustle")
                    .type(INCOME)
                    .icon("attach_money")
                    .userId(testUserId)
                    .build()));
        }

        @Test
        void provision_mixedInterests_callsServiceForKnownAndDefault() {
            Set<String> interests = Set.of("groceries", "unknown", "rent");

            provisioner.provision(interests, testUserId);

            // default (3) + groceries (1) + rent (2) = 6
            verify(categoryService, times(6)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
        }

        @Test
        void provision_incomeInterests_callsServiceWithIncomeCategoriesAndDefaults() {
            Set<String> interests = Set.of("salary_active", "passive_investments");

            provisioner.provision(interests, testUserId);

            // default (3) + salary_active (1) + passive_investments (1) = 5
            verify(categoryService, times(5)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
            verify(categoryService).createCategory(eq(CreateCategoryCommand.builder()
                    .name("Salary & Wage")
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
        void provision_multiCategoryInterests_callsServiceForAllSubCategoriesAndDefaults() {
            Set<String> interests = Set.of("shopping", "health_fitness");

            provisioner.provision(interests, testUserId);

            // default (3) + shopping (3) + health_fitness (2) = 8
            verify(categoryService, times(8)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService).deleteAllByUserId(testUserId);
        }

        @Test
        void provision_allInterests_callsServiceForAllCategories() {
            Set<String> interests = Set.of(
                    "groceries", "bills", "rent", "entertainment", "savings",
                    "shopping", "health_fitness", "family_pets", "debt_loans",
                    "salary_active", "business_sales", "passive_investments", "allowances_gifts"
            );

            provisioner.provision(interests, testUserId);

            // default (3) + groceries (1) + bills (2) + rent (2) + entertainment (2) + savings (1) +
            // shopping (3) + health_fitness (2) + family_pets (3) + debt_loans (2) +
            // salary_active (1) + business_sales (1) + passive_investments (1) + allowances_gifts (2) = 26
            verify(categoryService, times(26)).createCategory(any(CreateCategoryCommand.class));
            verify(categoryService, times(1)).deleteAllByUserId(testUserId);
        }
    }
}
