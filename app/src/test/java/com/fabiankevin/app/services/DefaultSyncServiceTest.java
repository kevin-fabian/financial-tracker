package com.fabiankevin.app.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class DefaultSyncServiceTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final RecurringTransactionService recurringTransactionService = mock(RecurringTransactionService.class);
    private final BudgetService budgetService = mock(BudgetService.class);
    private final ShoppingListService shoppingListService = mock(ShoppingListService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);

    private final DefaultSyncService syncService = new DefaultSyncService(
            transactionService,
            recurringTransactionService,
            budgetService,
            shoppingListService,
            accountService,
            categoryService
    );

    @Nested
    class Prepare {

        @Test
        void givenValidUserId_shouldDeleteAllDataInDependencyOrder() {
            UUID userId = UUID.randomUUID();

            syncService.prepare(userId);

            verify(transactionService).deleteAllByUserId(userId);
            verify(recurringTransactionService).deleteAllByUserId(userId);
            verify(budgetService).deleteAllByUserId(userId);
            verify(shoppingListService).deleteAllByUserId(userId);
            verify(accountService).deleteAllByUserId(userId);
            verify(categoryService).deleteAllByUserId(userId);
            verifyNoMoreInteractions(transactionService, recurringTransactionService, budgetService,
                    shoppingListService, accountService, categoryService);
        }

        @Test
        void givenTransactionServiceThrows_shouldPropagateExceptionAndNotCallRemainingServices() {
            UUID userId = UUID.randomUUID();
            doThrow(new RuntimeException("Service error")).when(transactionService)
                    .deleteAllByUserId(userId);

            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> syncService.prepare(userId)
            );

            verify(transactionService).deleteAllByUserId(userId);
            verifyNoInteractions(recurringTransactionService, budgetService,
                    shoppingListService, accountService, categoryService);
        }
    }
}
