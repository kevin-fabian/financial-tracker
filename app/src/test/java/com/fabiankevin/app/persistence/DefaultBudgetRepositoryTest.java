package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.BudgetEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaBudgetRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
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
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

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
                .user(User.of(userId))
                .updatedBy(User.of(userId))
                .period(BudgetPeriod.MONTHLY)
                .category(category)
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
            assertEquals(budget.period(), saved.period(), "period should match");
            assertEquals(budget.allocated(), saved.allocated(), "allocated should match");
            assertEquals(budget.user(), saved.user(), "user should match");
            assertEquals(budget.updatedBy(), saved.updatedBy(), "updatedBy should match");
            assertEquals(budget.category().icon(), saved.category().icon(), "categoryIcon should match");
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

    @Nested
    class FindBudgetSummaryById {
        @Test
        void givenExistingIdWithTransactions_returnsSummaryWithSpent() {
            // Arrange: save category entity first, then budget, then transactions
            CategoryEntity savedCategory = jpaCategoryRepository.save(CategoryEntity.from(budget.category()));

            BudgetEntity savedBudgetEntity = jpaBudgetRepository.save(BudgetEntity.builder()
                    .id(budget.id())
                    .userId(budget.user().id())
                    .updatedBy(budget.updatedBy() != null ? budget.updatedBy().id() : null)
                    .period(BudgetPeriod.MONTHLY)
                    .category(savedCategory)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());

            Instant now = Instant.now();
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            TransactionEntity transaction1 = TransactionEntity.builder()
                    .amount(150.0)
                    .transactionDate(monthStart.plusDays(5))
                    .category(savedCategory)
                    .addedBy(budget.user().id())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            TransactionEntity transaction2 = TransactionEntity.builder()
                    .amount(200.0)
                    .transactionDate(monthStart.plusDays(10))
                    .category(savedCategory)
                    .addedBy(budget.user().id())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            jpaTransactionRepository.saveAll(List.of(transaction1, transaction2));

            // Act
            Optional<BudgetSummary> summaryOpt = budgetRepository.findBudgetSummaryById(savedBudgetEntity.getId());

            // Assert
            Assertions.assertThat(summaryOpt)
                    .as("budget summary should be found")
                    .isPresent();

            BudgetSummary summary = summaryOpt.get();
            assertNotNull(summary.budget(), "budget should not be null");
            assertEquals(350.0, summary.spent(), "spent should be sum of transactions in current month");
            assertEquals(500.0, summary.budget().allocated(), "allocated should match budget");
            assertEquals(70.0, summary.spentPercentage(), "spentPercentage should be (350/500)*100");

            verify(jpaBudgetRepository, times(1)).findByBudgetId(savedBudgetEntity.getId(), monthStart, monthEnd);
        }

        @Test
        void givenExistingIdWithoutTransactions_returnsSummaryWithZeroSpent() {
            // Arrange: save budget directly via JPA without any transactions
            Instant now = Instant.now();
            CategoryEntity savedCategory = jpaCategoryRepository.save(CategoryEntity.from(budget.category()));
            BudgetEntity savedBudgetEntity = jpaBudgetRepository.save(BudgetEntity.builder()
                    .id(budget.id())
                    .userId(budget.user().id())
                    .updatedBy(budget.updatedBy() != null ? budget.updatedBy().id() : null)
                    .period(BudgetPeriod.MONTHLY)
                    .category(savedCategory)
                    .allocated(500.0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            // Act
            Optional<BudgetSummary> summaryOpt = budgetRepository.findBudgetSummaryById(savedBudgetEntity.getId());

            // Assert
            Assertions.assertThat(summaryOpt)
                    .as("budget summary should be found even without transactions")
                    .isPresent();

            BudgetSummary summary = summaryOpt.get();
            assertNotNull(summary.budget(), "budget should not be null");
            assertEquals(0.0, summary.spent(), "spent should be zero when no transactions exist");
            assertEquals(500.0, summary.budget().allocated(), "allocated should match budget");
            assertEquals(0.0, summary.spentPercentage(), "spentPercentage should be zero when no spending");

            verify(jpaBudgetRepository, times(1)).findByBudgetId(savedBudgetEntity.getId(), LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        }

        @Test
        void givenNonExistingId_returnsEmptyOptional() {
            // Act
            Optional<BudgetSummary> summaryOpt = budgetRepository.findBudgetSummaryById(UUID.randomUUID());

            // Assert
            Assertions.assertThat(summaryOpt)
                    .as("non-existing budget id should return empty optional")
                    .isEmpty();

            verify(jpaBudgetRepository, times(1)).findByBudgetId(any(), any(), any());
        }
    }

    @Nested
    class FindAllBudgetSummaryByUserId {
        @Test
        void givenBudgetsWithTransactions_returnsSummariesWithSpentAndPercentage() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            CategoryEntity category = jpaCategoryRepository.saveAndFlush(CategoryEntity.builder()
                    .name("GROCERIES")
                    .transactionType(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("local_grocery_store")
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaBudgetRepository.saveAndFlush(BudgetEntity.builder()
                    .userId(userId)
                    .updatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .category(category)
                    .amount(150.0)
                    .transactionDate(LocalDate.now())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .category(category)
                    .amount(50.0)
                    .transactionDate(LocalDate.now())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            ZonedDateTime monthStart = Instant.now().atZone(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
            LocalDate startMonth = monthStart.toLocalDate();
            LocalDate endMonth = startMonth.plusMonths(1);
            List<BudgetSummary> results = budgetRepository.findAllBudgetSummaryByUserId(List.of(userId), startMonth, endMonth);

            Assertions.assertThat(results)
                    .as("should return one budget summary")
                    .hasSize(1);
            BudgetSummary summary = results.getFirst();
            assertEquals(userId, summary.budget().user().id(), "user should match");
            assertEquals(BudgetPeriod.MONTHLY, summary.budget().period(), "period should match");
            assertEquals(500.0, summary.budget().allocated(), "allocated should match");
            assertEquals(200.0, summary.spent(), "spent should be sum of transactions");
            assertEquals(40.0, summary.spentPercentage(), "spentPercentage should be spent/allocated*100");
            assertNotNull(summary.budget().id(), "budget id should not be null");
            assertEquals("GROCERIES", summary.budget().category().name(), "category name should match");
        }

        @Test
        void givenBudgetsWithNoTransactions_returnsSummariesWithZeroSpent() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            CategoryEntity category = jpaCategoryRepository.saveAndFlush(CategoryEntity.builder()
                    .name("RENT")
                    .transactionType(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("home")
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaBudgetRepository.saveAndFlush(BudgetEntity.builder()
                    .userId(userId)
                    .updatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(1000.0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            ZonedDateTime monthStart = Instant.now().atZone(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
            LocalDate startMonth = monthStart.toLocalDate();
            LocalDate endMonth = startMonth.plusMonths(1);
            List<BudgetSummary> results = budgetRepository.findAllBudgetSummaryByUserId(List.of(userId), startMonth, endMonth);

            Assertions.assertThat(results)
                    .as("should return one budget summary")
                    .hasSize(1);
            BudgetSummary summary = results.getFirst();
            assertEquals(1000.0, summary.budget().allocated(), "allocated should match");
            assertEquals(0.0, summary.spent(), "spent should be zero with no transactions");
            assertEquals(0.0, summary.spentPercentage(), "spentPercentage should be zero with no spent");
        }

        @Test
        void givenTransactionsFromPreviousAndCurrentMonth_returnsOnlyCurrentMonthSpent() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            LocalDate today = LocalDate.now();
            LocalDate previousMonthStart = today.withDayOfMonth(1).minusMonths(1);

            CategoryEntity category = jpaCategoryRepository.saveAndFlush(CategoryEntity.builder()
                    .name("DINING")
                    .transactionType(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("restaurant")
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaBudgetRepository.saveAndFlush(BudgetEntity.builder()
                    .userId(userId)
                    .updatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(800.0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            // Previous month transaction — should NOT be included
            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .category(category)
                    .amount(300.0)
                    .transactionDate(previousMonthStart)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            // Current month transactions — should be included
            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .category(category)
                    .amount(100.0)
                    .transactionDate(today)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .category(category)
                    .amount(50.0)
                    .transactionDate(today)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            LocalDate startMonth = today.withDayOfMonth(1);
            LocalDate endMonth = startMonth.plusMonths(1);
            List<BudgetSummary> results = budgetRepository.findAllBudgetSummaryByUserId(List.of(userId), startMonth, endMonth);

            Assertions.assertThat(results)
                    .as("should return one budget summary")
                    .hasSize(1);
            BudgetSummary summary = results.getFirst();
            assertEquals(800.0, summary.budget().allocated(), "allocated should match");
            assertEquals(150.0, summary.spent(), "spent should only include current month transactions (100 + 50), excluding previous month (300)");
            assertEquals(18.75, summary.spentPercentage(), "spentPercentage should be 150/800*100 = 18.75");
        }
    }
}
