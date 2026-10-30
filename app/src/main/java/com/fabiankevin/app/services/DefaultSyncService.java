package com.fabiankevin.app.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSyncService implements SyncService {
    private final TransactionService transactionService;
    private final RecurringTransactionService recurringTransactionService;
    private final BudgetService budgetService;
    private final ShoppingListService shoppingListService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @Transactional
    @Override
    public void prepare(UUID userId) {
        log.info("Starting data synchronization for user: {}", userId);

        // 1. Delete Transactions first (depends on Accounts and Categories)
        transactionService.deleteAllByUserId(userId);
        log.debug("Deleted all transactions for user: {}", userId);

        // 2. Delete RecurringTransactions (depends on Accounts and Categories)
        recurringTransactionService.deleteAllByUserId(userId);
        log.debug("Deleted all recurring transactions for user: {}", userId);

        // 3. Delete Budgets (depends on Categories)
        budgetService.deleteAllByUserId(userId);
        log.debug("Deleted all budgets for user: {}", userId);

        // 4. Delete ShoppingLists (depends on Categories, contains ShoppingItems)
        shoppingListService.deleteAllByUserId(userId);
        log.debug("Deleted all shopping lists for user: {}", userId);

        // 5. Delete Accounts (depends on User)
        accountService.deleteAllByUserId(userId);
        log.debug("Deleted all accounts for user: {}", userId);

        // 6. Delete Categories last among dependencies (depends on User)
        categoryService.deleteAllByUserId(userId);
        log.debug("Deleted all categories for user: {}", userId);

        log.info("Data synchronization completed for user: {}", userId);
    }
}
