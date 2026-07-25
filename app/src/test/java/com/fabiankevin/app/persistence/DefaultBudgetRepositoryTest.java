package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.jpa_repositories.JpaBudgetRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(DefaultBudgetRepository.class)
class DefaultBudgetRepositoryTest {

    @MockitoSpyBean
    private JpaBudgetRepository jpaBudgetRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private Budget budget;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        Category category = Category.builder()
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("local_grocery_store")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        budget = Budget.builder()
                .userId(userId)
                .lastUpdatedBy(userId)
                .name("Monthly Groceries")
                .period(BudgetPeriod.MONTHLY)
                .category(category)
                .icon("savings")
                .allocated(500.0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.reset(jpaBudgetRepository);
    }

    @Nested
    class Save {
        @Test
        void givenValidBudget_persistsAndRetrievesAllFields() {
            Budget saved = budgetRepository.save(budget);

            assertNotNull(saved.id(), "budget id should have been generated");
            assertEquals(budget.name(), saved.name(), "name should match");
            assertEquals(budget.period(), saved.period(), "period should match");
            assertEquals(budget.allocated(), saved.allocated(), "allocated should match");
            assertEquals(budget.userId(), saved.userId(), "userId should match");
            assertEquals(budget.lastUpdatedBy(), saved.lastUpdatedBy(), "lastUpdatedBy should match");
            assertEquals(budget.icon(), saved.icon(), "icon should match");
            assertNotNull(saved.createdAt(), "createdAt should not be null");
            assertNotNull(saved.updatedAt(), "updatedAt should not be null");
            assertNotNull(saved.category(), "category should not be null");
            assertEquals(budget.category().name(), saved.category().name(), "category name should match");

            verify(jpaBudgetRepository, times(1)).save(any());
        }

        @Test
        void givenNull_shouldThrowInvalidDataAccessApiUsageException() {
            Assertions.assertThatThrownBy(() -> budgetRepository.save(null))
                    .as("saving null should throw InvalidDataAccessApiUsageException")
                    .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);
        }
    }

    @Nested
    class FindById {
        @Test
        void givenExistingId_returnsBudget() {
            Budget saved = budgetRepository.save(budget);

            Optional<Budget> found = budgetRepository.findById(saved.id());

            Assertions.assertThat(found)
                    .as("budget should be found by id")
                    .isPresent();
            Assertions.assertThat(found.get())
                    .as("retrieved budget should match saved budget")
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(saved);

            verify(jpaBudgetRepository, times(1)).findById(saved.id());
        }

        @Test
        void givenNonExistingId_returnsEmpty() {
            Optional<Budget> found = budgetRepository.findById(UUID.randomUUID());

            Assertions.assertThat(found)
                    .as("no budget should be found for unknown id")
                    .isEmpty();

            verify(jpaBudgetRepository, times(1)).findById(any());
        }
    }
}
