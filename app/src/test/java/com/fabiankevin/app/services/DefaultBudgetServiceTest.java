package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultBudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private DefaultBudgetService budgetService;

    private UUID userId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("local_grocery_store")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    class CreateBudget {
        @Test
        void givenValidCommand_thenCreatesAndReturnsBudgetSummary() {
            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(false);
            when(categoryService.getCategoryById(categoryId, userId)).thenReturn(category);
            when(budgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.sumSpentByCategoryIdAndUserId(eq(categoryId), eq(userId))).thenReturn(200.0);

            BudgetSummary created = budgetService.createBudget(command);

            assertNotNull(created.budget().id(), "id should be generated");
            assertEquals(userId, created.budget().user().id(), "user should match command");
            assertEquals(BudgetPeriod.MONTHLY, created.budget().period(), "period should match command");
            assertEquals(categoryId, created.budget().category().id(), "categoryId should match command");
            assertEquals("GROCERIES", created.budget().category().name(), "categoryName should be resolved from category");
            assertEquals("local_grocery_store", created.budget().category().icon(), "categoryIcon should be resolved from category");
            assertEquals(500.0, created.budget().allocated(), "allocated should match command");
            assertEquals(200.0, created.spent(), "spent should reflect summed transactions for the category");
            assertEquals(40.0, created.spentPercentage(), "spentPercentage should be spent/allocated*100");

            verify(categoryService, times(1)).getCategoryById(categoryId, userId);
            verify(budgetRepository, times(1)).save(any());
            verify(transactionRepository, times(1)).sumSpentByCategoryIdAndUserId(eq(categoryId), eq(userId));
        }

        @Test
        void givenCategoryNotFound_thenThrowsCategoryNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(categoryService.getCategoryById(categoryId, userId)).thenThrow(new CategoryNotFoundException());

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryService, times(1)).getCategoryById(categoryId, userId);
            verify(budgetRepository, never()).save(any());
        }

        @Test
        void givenCategoryAlreadyHasBudgetThisMonth_thenThrowsBudgetAlreadyExistException() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId))
                    .thenReturn(true);

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(BudgetAlreadyExistException.class);

            verify(budgetRepository, times(1)).existsByCategoryIdAndUserId(categoryId, userId);
            verify(categoryService, never()).getCategoryById(any(), any());
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class GetBudgetsByUserId {
        @Test
        void givenExistingBudgets_thenReturnsSummaries() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            BudgetSummary summary = BudgetSummary.builder()
                    .budget(budget)
                    .spent(200.0)
                    .spentPercentage(40.0)
                    .build();

            when(budgetRepository.findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(summary));

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).hasSize(1);
            BudgetSummary result = results.getFirst();

            // identity & ownership fields preserved from nested budget
            assertEquals(budgetId, result.budget().id(), "id should be preserved");
            assertEquals(userId, result.budget().user().id(), "user should be preserved");

            // timestamps & period preserved from nested budget
            assertNotNull(result.budget().createdAt(), "createdAt should be preserved from source budget");
            assertNotNull(result.budget().updatedAt(), "updatedAt should be preserved from source budget");
            assertEquals(BudgetPeriod.MONTHLY, result.budget().period(), "period should be preserved");

            // category fields preserved from nested budget
            assertEquals(categoryId, result.budget().category().id(), "categoryId should be preserved");
            assertEquals("GROCERIES", result.budget().category().name(), "categoryName should be preserved");
            assertEquals("local_grocery_store", result.budget().category().icon(), "categoryIcon should be preserved");

            // amount fields
            assertEquals(500.0, result.budget().allocated(), "allocated should be preserved");
            assertEquals(200.0, result.spent(), "spent should be preserved");
            assertEquals(40.0, result.spentPercentage(), "spentPercentage should be preserved");

            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        void givenNoBudgets_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(budgetRepository.findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).isEmpty();
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    class RecreateBudgetsFromLastMonth {
        @Test
        void givenLastMonthHasBudgets_thenRecreatesSameCountForCurrentMonth() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Category category = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Instant lastMonth = Instant.now().atZone(java.time.ZoneOffset.UTC).minusMonths(1).toInstant();
            Budget lastMonthBudget = Budget.builder()
                    .id(UUID.randomUUID())
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(lastMonth)
                    .updatedAt(lastMonth)
                    .build();

            when(budgetRepository.findAllByUserIdAndCreatedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(lastMonthBudget));
            when(categoryService.getCategoryById(categoryId, userId)).thenReturn(category);
            when(budgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.sumSpentByCategoryIdAndUserId(eq(categoryId), eq(userId))).thenReturn(0.0);

            List<BudgetSummary> results = budgetService.recreateBudgetsFromLastMonth(userId);

            assertThat(results).hasSize(1);
            BudgetSummary recreated = results.getFirst();
            assertEquals(categoryId, recreated.budget().category().id());
            assertEquals(BudgetPeriod.MONTHLY, recreated.budget().period());
            assertEquals(500.0, recreated.budget().allocated());
            assertEquals(0.0, recreated.spent());
            verify(budgetRepository, times(1)).findAllByUserIdAndCreatedAtBetween(eq(userId), any(Instant.class), any(Instant.class));
            verify(budgetRepository, times(1)).save(any());
        }

        @Test
        void givenLastMonthHasNoBudgets_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(budgetRepository.findAllByUserIdAndCreatedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());

            List<BudgetSummary> results = budgetService.recreateBudgetsFromLastMonth(userId);

            assertThat(results).isEmpty();
            verify(budgetRepository, times(1)).findAllByUserIdAndCreatedAtBetween(eq(userId), any(Instant.class), any(Instant.class));
            verify(budgetRepository, never()).save(any());
            verify(categoryService, never()).getCategoryById(any(), any());
        }
    }

    @Nested
    class PatchBudget {
        @Test
        void givenValidCommand_thenUpdatesProvidedFieldsAndReturnsBudgetSummary() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = Category.builder()
                    .id(newCategoryId)
                    .name("RENT")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("home")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget existing = Budget.builder()
                    .id(id)
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(Category.builder()
                            .id(UUID.randomUUID())
                            .name("GROCERIES")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .icon("local_grocery_store")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(id)
                    .categoryId(newCategoryId)
                    .period(BudgetPeriod.YEARLY)
                    .allocated(1000.0)
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
            when(categoryService.getCategoryById(newCategoryId, userId)).thenReturn(newCategory);
            when(budgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.sumSpentByCategoryIdAndUserId(eq(newCategoryId), eq(userId))).thenReturn(200.0);

            BudgetSummary updated = budgetService.patchBudget(command);

            // identity & ownership fields
            assertEquals(id, updated.budget().id(), "id should be preserved");
            assertEquals(userId, updated.budget().user().id(), "user should be preserved");
            assertEquals(userId, updated.budget().updatedBy().id(), "updatedBy should be set");

            // timestamps
            assertNotNull(updated.budget().createdAt(), "createdAt should be preserved from existing budget");
            assertNotNull(updated.budget().updatedAt(), "updatedAt should not be null");

            // period & category fields
            assertEquals(BudgetPeriod.YEARLY, updated.budget().period(), "period should be updated");
            assertEquals(newCategoryId, updated.budget().category().id(), "categoryId should be updated");
            assertEquals("RENT", updated.budget().category().name(), "categoryName should be updated");
            assertEquals("home", updated.budget().category().icon(), "categoryIcon should be updated");

            // amount fields
            assertEquals(1000.0, updated.budget().allocated(), "allocated should be updated");
            assertEquals(200.0, updated.spent(), "spent should reflect summed transactions");
            assertEquals(20.0, updated.spentPercentage(), "spentPercentage should be 200/1000*100");

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(categoryService, times(1)).getCategoryById(newCategoryId, userId);
            verify(budgetRepository, times(1)).save(any());
        }

        @Test
        void givenBudgetNotFound_thenThrowsBudgetNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(id)
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.patchBudget(command))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteBudget {
        @Test
        void givenValidIdAndUserId_thenDeletesBudget() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            budgetService.deleteBudgetById(id, userId);

            verify(budgetRepository, times(1)).deleteByIdAndUserId(id, userId);
        }
    }
}
