package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.persistence.SyncStateRepository;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DefaultSyncServiceTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final RecurringTransactionService recurringTransactionService = mock(RecurringTransactionService.class);
    private final BudgetService budgetService = mock(BudgetService.class);
    private final ShoppingListService shoppingListService = mock(ShoppingListService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final SyncStateRepository syncStateRepository = mock(SyncStateRepository.class);

    private final DefaultSyncService syncService = new DefaultSyncService(
            transactionService,
            recurringTransactionService,
            budgetService,
            shoppingListService,
            accountService,
            categoryService,
            syncStateRepository
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

            assertThatThrownBy(() -> syncService.prepare(userId))
                    .isInstanceOf(RuntimeException.class);

            verify(transactionService).deleteAllByUserId(userId);
            verifyNoInteractions(recurringTransactionService, budgetService,
                    shoppingListService, accountService, categoryService);
        }
    }

    @Nested
    class CreateSyncState {

        @Test
        void givenValidCommand_shouldCreateAndSaveSyncState() {
            UUID userId = UUID.randomUUID();
            CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                    .userId(userId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.PENDING)
                    .build();

            when(syncStateRepository.save(any(SyncState.class))).thenAnswer(invocation -> {
                SyncState input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).build();
            });

            SyncState result = syncService.createSyncState(command);

            assertThat(result.id()).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.userDevice()).isEqualTo("iphone-15");
            assertThat(result.resourceType()).isEqualTo(ResourceType.ACCOUNTS);
            assertThat(result.syncStatus()).isEqualTo(SyncStatus.PENDING);
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
            verify(syncStateRepository).save(any(SyncState.class));
        }

        @Test
        void givenNullSyncStatus_shouldDefaultToPending() {
            UUID userId = UUID.randomUUID();
            CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                    .userId(userId)
                    .userDevice("android-device")
                    .resourceType(ResourceType.CATEGORIES)
                    .syncStatus(null)
                    .build();

            when(syncStateRepository.save(any(SyncState.class))).thenAnswer(invocation -> {
                SyncState input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).build();
            });

            SyncState result = syncService.createSyncState(command);

            assertThat(result.syncStatus()).isEqualTo(SyncStatus.PENDING);
        }
    }

    @Nested
    class PatchSyncState {

        @Test
        void givenExistingSyncState_shouldUpdateStatusAndTimestamp() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncState existing = SyncState.builder()
                    .id(syncId)
                    .userId(userId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncStateRepository.findById(syncId)).thenReturn(Optional.of(existing));
            when(syncStateRepository.save(any(SyncState.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            SyncState result = syncService.patchSyncState(command);

            assertThat(result.id()).isEqualTo(syncId);
            assertThat(result.syncStatus()).isEqualTo(SyncStatus.COMPLETED);
            assertThat(result.updatedAt()).isAfterOrEqualTo(now);
            verify(syncStateRepository).save(any(SyncState.class));
        }

        @Test
        void givenNonExistentSyncState_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();

            when(syncStateRepository.findById(syncId)).thenReturn(Optional.empty());

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            assertThatThrownBy(() -> syncService.patchSyncState(command))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }

        @Test
        void givenSyncStateOwnedByDifferentUser_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncState existing = SyncState.builder()
                    .id(syncId)
                    .userId(otherUserId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncStateRepository.findById(syncId)).thenReturn(Optional.of(existing));

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            assertThatThrownBy(() -> syncService.patchSyncState(command))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }
    }

    @Nested
    class GetSyncStateById {

        @Test
        void givenExistingSyncStateOwnedByUser_shouldReturnSyncState() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncState existing = SyncState.builder()
                    .id(syncId)
                    .userId(userId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.TRANSACTIONS)
                    .syncStatus(SyncStatus.IN_PROGRESS)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncStateRepository.findById(syncId)).thenReturn(Optional.of(existing));

            SyncState result = syncService.getSyncStateById(syncId, userId);

            assertThat(result.id()).isEqualTo(syncId);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.resourceType()).isEqualTo(ResourceType.TRANSACTIONS);
            assertThat(result.syncStatus()).isEqualTo(SyncStatus.IN_PROGRESS);
        }

        @Test
        void givenNonExistentSyncState_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();

            when(syncStateRepository.findById(syncId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> syncService.getSyncStateById(syncId, userId))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }
    }

    @Nested
    class GetAllSyncStatesByUserId {

        @Test
        void givenUserWithSyncStates_shouldReturnAllSyncStates() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncState state1 = SyncState.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.COMPLETED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            SyncState state2 = SyncState.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.CATEGORIES)
                    .syncStatus(SyncStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncStateRepository.findAllByUserId(userId)).thenReturn(List.of(state1, state2));

            List<SyncState> result = syncService.getAllSyncStatesByUserId(userId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SyncState::resourceType)
                    .containsExactly(ResourceType.ACCOUNTS, ResourceType.CATEGORIES);
        }

        @Test
        void givenUserWithNoSyncStates_shouldReturnEmptyList() {
            UUID userId = UUID.randomUUID();

            when(syncStateRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<SyncState> result = syncService.getAllSyncStatesByUserId(userId);

            assertThat(result).isEmpty();
        }
    }
}
