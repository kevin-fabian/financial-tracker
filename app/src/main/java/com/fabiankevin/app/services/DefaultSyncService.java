package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.persistence.SyncStateRepository;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSyncService implements SyncService {
    private final TransactionService transactionService;
    private final RecurringTransactionService recurringTransactionService;
    private final BudgetService budgetService;
    private final ShoppingListService shoppingListService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SyncStateRepository syncStateRepository;

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

    @Transactional
    @Override
    public SyncState createSyncState(CreateSyncStateCommand command) {
        SyncStatus status = command.syncStatus() != null ? command.syncStatus() : SyncStatus.PENDING;
        SyncState syncState = SyncState.builder()
                .userId(command.userId())
                .userDevice(command.userDevice())
                .resourceType(command.resourceType())
                .syncStatus(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return syncStateRepository.save(syncState);
    }

    @Transactional
    @Override
    public SyncState patchSyncState(PatchSyncStateCommand command) {
        SyncState existing = syncStateRepository.findById(command.id())
                .filter(s -> s.userId().equals(command.userId()))
                .orElseThrow(SyncStateNotFoundException::new);

        SyncState updated = existing.toBuilder()
                .syncStatus(command.syncStatus())
                .updatedAt(Instant.now())
                .build();
        return syncStateRepository.save(updated);
    }

    @Override
    public SyncState getSyncStateById(UUID id, UUID userId) {
        return syncStateRepository.findById(id)
                .filter(s -> s.userId().equals(userId))
                .orElseThrow(SyncStateNotFoundException::new);
    }

    @Override
    public List<SyncState> getAllSyncStatesByUserId(UUID userId) {
        return syncStateRepository.findAllByUserId(userId);
    }
}
