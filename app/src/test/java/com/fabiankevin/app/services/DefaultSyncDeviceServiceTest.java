package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import com.fabiankevin.app.persistence.SyncDeviceRepository;
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

class DefaultSyncDeviceServiceTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final RecurringTransactionService recurringTransactionService = mock(RecurringTransactionService.class);
    private final BudgetService budgetService = mock(BudgetService.class);
    private final ShoppingListService shoppingListService = mock(ShoppingListService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final SyncDeviceRepository syncDeviceRepository = mock(SyncDeviceRepository.class);

    private final DefaultSyncDeviceService syncService = new DefaultSyncDeviceService(
            transactionService,
            recurringTransactionService,
            budgetService,
            shoppingListService,
            accountService,
            categoryService,
            syncDeviceRepository
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
    class CreateSyncDevice {

        @Test
        void givenValidCommand_shouldCreateAndSaveSyncState() {
            UUID userId = UUID.randomUUID();
            CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                    .userId(userId)
                    .userDeviceId(UUID.randomUUID())
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .build();

            when(syncDeviceRepository.save(any(SyncDevice.class))).thenAnswer(invocation -> {
                SyncDevice input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).build();
            });

            SyncDevice result = syncService.create(command);

            assertThat(result.id()).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.deviceId()).isNotNull();
            assertThat(result.userDevice()).isEqualTo(command.userDeviceId());
            assertThat(result.status()).isEqualTo(BootstrapStatus.IN_PROGRESS);
            assertThat(result.direction()).isEqualTo(BootstrapDirection.PULL);
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
            verify(syncDeviceRepository).save(any(SyncDevice.class));
        }
    }

    @Nested
    class PatchSyncDevice {

        @Test
        void givenExistingSyncState_shouldUpdateStatusAndTimestamp() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncDevice existing = SyncDevice.builder()
                    .id(syncId)
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncDeviceRepository.findById(syncId)).thenReturn(Optional.of(existing));
            when(syncDeviceRepository.save(any(SyncDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(BootstrapStatus.COMPLETED)
                    .build();

            SyncDevice result = syncService.patch(command);

            assertThat(result.id()).isEqualTo(syncId);
            assertThat(result.status()).isEqualTo(BootstrapStatus.COMPLETED);
            assertThat(result.updatedAt()).isAfterOrEqualTo(now);
            verify(syncDeviceRepository).save(any(SyncDevice.class));
        }

        @Test
        void givenNonExistentSyncState_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();

            when(syncDeviceRepository.findById(syncId)).thenReturn(Optional.empty());

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(BootstrapStatus.COMPLETED)
                    .build();

            assertThatThrownBy(() -> syncService.patch(command))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }

        @Test
        void givenSyncStateOwnedByDifferentUser_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncDevice existing = SyncDevice.builder()
                    .id(syncId)
                    .userId(otherUserId)
                    .deviceId(UUID.randomUUID())
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncDeviceRepository.findById(syncId)).thenReturn(Optional.of(existing));

            PatchSyncStateCommand command = PatchSyncStateCommand.builder()
                    .id(syncId)
                    .userId(userId)
                    .syncStatus(BootstrapStatus.COMPLETED)
                    .build();

            assertThatThrownBy(() -> syncService.patch(command))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }
    }

    @Nested
    class GetSyncDeviceById {

        @Test
        void givenExistingSyncStateOwnedByUser_shouldReturnSyncState() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncDevice existing = SyncDevice.builder()
                    .id(syncId)
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PUSH)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncDeviceRepository.findById(syncId)).thenReturn(Optional.of(existing));

            SyncDevice result = syncService.getById(syncId, userId);

            assertThat(result.id()).isEqualTo(syncId);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.status()).isEqualTo(BootstrapStatus.IN_PROGRESS);
            assertThat(result.direction()).isEqualTo(BootstrapDirection.PUSH);
        }

        @Test
        void givenNonExistentSyncState_shouldThrowNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID syncId = UUID.randomUUID();

            when(syncDeviceRepository.findById(syncId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> syncService.getById(syncId, userId))
                    .isInstanceOf(SyncStateNotFoundException.class);
        }
    }

    @Nested
    class GetAllSyncStatesByUserId {

        @Test
        void givenUserWithSyncStates_shouldReturnAllSyncStates() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            SyncDevice state1 = SyncDevice.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .status(BootstrapStatus.COMPLETED)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            SyncDevice state2 = SyncDevice.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PUSH)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(syncDeviceRepository.findAllByUserId(userId)).thenReturn(List.of(state1, state2));

            List<SyncDevice> result = syncService.getAllByUserId(userId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SyncDevice::status)
                    .containsExactly(BootstrapStatus.COMPLETED, BootstrapStatus.IN_PROGRESS);
        }

        @Test
        void givenUserWithNoSyncStates_shouldReturnEmptyList() {
            UUID userId = UUID.randomUUID();

            when(syncDeviceRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<SyncDevice> result = syncService.getAllByUserId(userId);

            assertThat(result).isEmpty();
        }
    }
}
