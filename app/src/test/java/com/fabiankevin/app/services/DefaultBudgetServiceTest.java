package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

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
    }
}
