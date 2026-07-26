package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
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
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultBudgetServiceTest {
    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultBudgetService budgetService;

    @Nested
    class CreateBudget {
        @Test
        void givenValidCommand_thenCreatesAndReturnsBudgetSummary() {
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

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            UUID generatedId = UUID.randomUUID();
            when(categoryService.getCategoryById(categoryId, userId)).thenReturn(category);
            when(budgetRepository.save(any())).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                return b.toBuilder().id(generatedId).build();
            });
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            BudgetSummary created = budgetService.createBudget(command);

            assertEquals(generatedId, created.id(), "budget id should have been generated");
            assertEquals(userId, created.userId(), "userId should match command");
            assertEquals(userId, created.lastUpdatedBy(), "lastUpdatedBy should be set to userId");
            assertEquals("John Doe", created.lastUpdatedByName(), "lastUpdatedByName should be enriched from UserClient");
            assertNotNull(created.updatedAt(), "updatedAt should not be null");
            assertEquals(BudgetPeriod.MONTHLY, created.period(), "period should match command");
            assertEquals(categoryId, created.categoryId(), "categoryId should be resolved from category");
            assertEquals("GROCERIES", created.categoryName(), "categoryName should be resolved from category");
            assertEquals("local_grocery_store", created.categoryIcon(), "categoryIcon should be resolved from category");
            assertEquals(500.0, created.allocated(), "allocated should match command");
            assertEquals(0.0, created.spent(), "spent should be zero for a newly created budget");
            assertEquals(0.0, created.spentPercentage(), "spentPercentage should be zero for a newly created budget");

            verify(categoryService, times(1)).getCategoryById(categoryId, userId);
            verify(budgetRepository, times(1)).save(any());
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
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

            when(budgetRepository.existsByCategoryIdAndUserIdAndCreatedAtBetween(eq(categoryId), eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(true);

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(BudgetAlreadyExistException.class);

            verify(budgetRepository, times(1)).existsByCategoryIdAndUserIdAndCreatedAtBetween(eq(categoryId), eq(userId), any(Instant.class), any(Instant.class));
            verify(categoryService, never()).getCategoryById(any(), any());
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class GetBudgetsByUserId {
        @Test
        void givenExistingBudgets_thenReturnsSummariesEnrichedWithLastUpdatedByName() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            BudgetSummary summary = BudgetSummary.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .lastUpdatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .categoryName("GROCERIES")
                    .categoryIcon("local_grocery_store")
                    .allocated(500.0)
                    .spent(200.0)
                    .spentPercentage(40.0)
                    .build();

            User user = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(budgetRepository.findAllBudgetSummaryByUserId(List.of(userId))).thenReturn(List.of(summary));
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(List.of(user));

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).hasSize(1);
            BudgetSummary result = results.getFirst();
            assertEquals(summary.id(), result.id());
            assertEquals(summary.userId(), result.userId());
            assertEquals(summary.lastUpdatedBy(), result.lastUpdatedBy());
            assertEquals("John Doe", result.lastUpdatedByName(), "lastUpdatedByName should be enriched from UserClient");
            assertEquals(summary.allocated(), result.allocated());
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(List.of(userId));
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
        }

        @Test
        void givenNoBudgets_thenReturnsEmptyListAndDoesNotCallUserClient() {
            UUID userId = UUID.randomUUID();

            when(budgetRepository.findAllBudgetSummaryByUserId(List.of(userId))).thenReturn(List.of());

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).isEmpty();
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(List.of(userId));
            verify(userClient, never()).getUsersByIds(any());
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

            Instant lastMonth = Instant.now().atZone(ZoneOffset.UTC).minusMonths(1).toInstant();
            Budget lastMonthBudget = Budget.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .lastUpdatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(lastMonth)
                    .updatedAt(lastMonth)
                    .build();

            when(budgetRepository.findAllByUserIdAndCreatedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(lastMonthBudget));
            when(budgetRepository.existsByCategoryIdAndUserIdAndCreatedAtBetween(eq(categoryId), eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(false);
            when(categoryService.getCategoryById(categoryId, userId)).thenReturn(category);
            when(budgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            List<BudgetSummary> results = budgetService.recreateBudgetsFromLastMonth(userId);

            assertThat(results).hasSize(1);
            BudgetSummary recreated = results.getFirst();
            assertEquals(categoryId, recreated.categoryId());
            assertEquals(BudgetPeriod.MONTHLY, recreated.period());
            assertEquals(500.0, recreated.allocated());
            assertEquals(0.0, recreated.spent());
            assertEquals("John Doe", recreated.lastUpdatedByName());
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
        void givenValidCommand_thenUpdatesProvidedFieldsAndReturnsBudget() {
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
                    .userId(userId)
                    .lastUpdatedBy(userId)
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

            Budget updated = budgetService.patchBudget(command);

            assertEquals(id, updated.id(), "id should be preserved");
            assertEquals(userId, updated.userId(), "userId should be preserved");
            assertEquals(userId, updated.lastUpdatedBy(), "lastUpdatedBy should be set to userId");
            assertEquals(BudgetPeriod.YEARLY, updated.period(), "period should be updated");
            assertEquals(newCategory, updated.category(), "category should be updated");
            assertEquals(1000.0, updated.allocated(), "allocated should be updated");
            assertNotNull(updated.updatedAt(), "updatedAt should not be null");

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
                    .allocated(1000.0)
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.patchBudget(command))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(categoryService, never()).getCategoryById(any(), any());
            verify(budgetRepository, never()).save(any());
        }

        @Test
        void givenCategoryAlreadyHasBudget_thenThrowsBudgetAlreadyExistException() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();

            Budget existing = Budget.builder()
                    .id(id)
                    .userId(userId)
                    .lastUpdatedBy(userId)
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
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
            when(budgetRepository.existsByCategoryIdAndUserId(newCategoryId, userId)).thenReturn(true);

            assertThatThrownBy(() -> budgetService.patchBudget(command))
                    .isInstanceOf(BudgetAlreadyExistException.class);

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(budgetRepository, times(1)).existsByCategoryIdAndUserId(newCategoryId, userId);
            verify(categoryService, never()).getCategoryById(any(), any());
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteBudgetById {
        @Test
        void givenExistingBudget_thenDeletesBudget() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            when(budgetRepository.deleteByIdAndUserId(id, userId)).thenReturn(1);

            budgetService.deleteBudgetById(id, userId);

            verify(budgetRepository, times(1)).deleteByIdAndUserId(id, userId);
        }

        @Test
        void givenNonExistingBudget_thenReturnsWithoutError() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            when(budgetRepository.deleteByIdAndUserId(id, userId)).thenReturn(0);

            budgetService.deleteBudgetById(id, userId);

            verify(budgetRepository, times(1)).deleteByIdAndUserId(id, userId);
        }
    }
}
