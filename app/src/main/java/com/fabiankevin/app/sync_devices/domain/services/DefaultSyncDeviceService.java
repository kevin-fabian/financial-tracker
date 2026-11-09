package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.services.ShoppingListService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncDevice;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncDeviceRepository;
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
public class DefaultSyncDeviceService implements SyncDeviceService {
    private final TransactionService transactionService;
    private final RecurringTransactionService recurringTransactionService;
    private final BudgetService budgetService;
    private final ShoppingListService shoppingListService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SyncDeviceRepository syncDeviceRepository;

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
    public SyncDevice create(CreateSyncStateCommand command) {
        return syncDeviceRepository.findByUserIdAndUserDevice(command.userId(), command.userDevice())
                .orElseGet(() -> {
                    SyncDevice syncDevice = SyncDevice.builder()
                            .userId(command.userId())
                            .userDevice(command.userDevice())
                            .status(command.status())
                            .direction(command.direction())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return syncDeviceRepository.save(syncDevice);
                });
    }

    @Transactional
    @Override
    public SyncDevice patch(PatchSyncStateCommand command) {
        SyncDevice existing = syncDeviceRepository.findById(command.id())
                .filter(s -> s.userId().equals(command.userId()))
                .orElseThrow(SyncStateNotFoundException::new);

        SyncDevice updated = existing.toBuilder()
                .status(command.syncStatus())
                .updatedAt(Instant.now())
                .build();
        return syncDeviceRepository.save(updated);
    }

    @Override
    public SyncDevice getById(UUID id, UUID userId) {
        return syncDeviceRepository.findById(id)
                .filter(s -> s.userId().equals(userId))
                .orElseThrow(SyncStateNotFoundException::new);
    }

    @Override
    public List<SyncDevice> getAllByUserId(UUID userId) {
        return syncDeviceRepository.findAllByUserId(userId);
    }
}
