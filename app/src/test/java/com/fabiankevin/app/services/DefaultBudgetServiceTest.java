package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
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

    @InjectMocks
    private DefaultBudgetService budgetService;

    @Nested
    class CreateBudget {
        @Test
        void givenValidCommand_thenCreatesAndReturnsBudget() {
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

            when(categoryService.getCategoryById(categoryId, userId)).thenReturn(category);
            when(budgetRepository.save(any())).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                return b.toBuilder().id(UUID.randomUUID()).build();
            });

            Budget created = budgetService.createBudget(command);

            assertNotNull(created.id(), "budget id should have been generated");
            assertEquals(userId, created.userId(), "userId should match command");
            assertEquals(userId, created.lastUpdatedBy(), "lastUpdatedBy should be set to userId");
            assertEquals(BudgetPeriod.MONTHLY, created.period(), "period should match command");
            assertEquals(category, created.category(), "category should be resolved from service");
            assertEquals("local_grocery_store", created.category().icon(), "categoryIcon should match command");
            assertEquals(500.0, created.allocated(), "allocated should match command");
            assertNotNull(created.createdAt(), "createdAt should not be null");
            assertNotNull(created.updatedAt(), "updatedAt should not be null");

            verify(categoryService, times(1)).getCategoryById(categoryId, userId);
            verify(budgetRepository, times(1)).save(any());
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
        void givenCategoryAlreadyHasBudget_thenThrowsBudgetAlreadyExistException() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(true);

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

            when(budgetRepository.findAllBudgetSummaryByUserId(List.of(userId))).thenReturn(List.of(summary));

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).hasSize(1);
            assertEquals(summary, results.getFirst(), "returned summary should match");
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(List.of(userId));
        }

        @Test
        void givenNoBudgets_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(budgetRepository.findAllBudgetSummaryByUserId(List.of(userId))).thenReturn(List.of());

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).isEmpty();
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(List.of(userId));
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
