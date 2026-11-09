package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.services.ShoppingListService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import com.fabiankevin.app.sync_devices.domain.models.BootstrapDirection;
import com.fabiankevin.app.sync_devices.domain.models.BootstrapStatus;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncDevice;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncDeviceRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final HouseholdService householdService = mock(HouseholdService.class);
    private final SyncDeviceRepository syncDeviceRepository = mock(SyncDeviceRepository.class);
    private final SyncChangeRepository syncChangeRepository = mock(SyncChangeRepository.class);

    private final DefaultSyncDeviceService syncService = new DefaultSyncDeviceService(
            transactionService,
            recurringTransactionService,
            budgetService,
            shoppingListService,
            accountService,
            categoryService,
            householdService,
            syncDeviceRepository,
            syncChangeRepository
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
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .build();

            when(syncDeviceRepository.findByUserIdAndUserDevice(userId, command.userDevice())).thenReturn(Optional.empty());
            when(syncDeviceRepository.save(any(SyncDevice.class))).thenAnswer(invocation -> {
                SyncDevice input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).build();
            });

            SyncDevice result = syncService.create(command);

            assertThat(result.id()).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.userDevice()).isEqualTo(command.userDevice());
            assertThat(result.status()).isEqualTo(BootstrapStatus.IN_PROGRESS);
            assertThat(result.direction()).isEqualTo(BootstrapDirection.PULL);
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
            verify(syncDeviceRepository).findByUserIdAndUserDevice(userId, command.userDevice());
            verify(syncDeviceRepository).save(any(SyncDevice.class));
        }

        @Test
        void givenExistingDevice_shouldReturnExistingSyncState() {
            UUID userId = UUID.randomUUID();
            CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                    .userId(userId)
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .build();

            SyncDevice existing = SyncDevice.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .userDevice(command.userDevice())
                    .status(BootstrapStatus.COMPLETED)
                    .direction(BootstrapDirection.PUSH)
                    .createdAt(Instant.now().minusSeconds(3600))
                    .updatedAt(Instant.now().minusSeconds(1800))
                    .build();

            when(syncDeviceRepository.findByUserIdAndUserDevice(userId, command.userDevice())).thenReturn(Optional.of(existing));

            SyncDevice result = syncService.create(command);

            assertThat(result.id()).isEqualTo(existing.id());
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.userDevice()).isEqualTo(command.userDevice());
            assertThat(result.status()).isEqualTo(BootstrapStatus.COMPLETED);
            assertThat(result.direction()).isEqualTo(BootstrapDirection.PUSH);
            assertThat(result.createdAt()).isEqualTo(existing.createdAt());
            assertThat(result.updatedAt()).isEqualTo(existing.updatedAt());
            verify(syncDeviceRepository).findByUserIdAndUserDevice(userId, command.userDevice());
            verify(syncDeviceRepository, org.mockito.Mockito.never()).save(any());
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
                    .userDevice("test-device-001")
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
                    .userDevice("test-device-001")
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
                    .userDevice("test-device-001")
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
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.COMPLETED)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            SyncDevice state2 = SyncDevice.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .userDevice("test-device-002")
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

    @Nested
    class PullChanges {

        @Test
        void givenEmptyCursors_shouldReturnEmptyMapWithoutCallingRepository() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<SyncCursorQuery> cursors = List.of();

            Map<UUID, List<SyncChange>> result = syncService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
        }

        @Test
        void givenCursorsWithInvalidSequence_shouldFilterOutInvalidCursors() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            SyncCursorQuery invalidCursor = SyncCursorQuery.builder()
                    .ownerId(userId)
                    .lastPulledSequence(-1L)
                    .build();

            List<SyncCursorQuery> cursors = List.of(invalidCursor);

            Map<UUID, List<SyncChange>> result = syncService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
        }

        @Test
        void givenCursorsWithNullSequence_shouldFilterOutNullCursors() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            SyncCursorQuery nullSequenceCursor = SyncCursorQuery.builder()
                    .ownerId(userId)
                    .lastPulledSequence(null)
                    .build();

            List<SyncCursorQuery> cursors = List.of(nullSequenceCursor);

            Map<UUID, List<SyncChange>> result = syncService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
        }

        @Test
        void givenMixedValidAndInvalidCursors_shouldOnlyPassValidToRepository() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            SyncCursorQuery validCursor = SyncCursorQuery.builder()
                    .ownerId(userId)
                    .lastPulledSequence(1L)
                    .build();

            SyncCursorQuery invalidCursor = SyncCursorQuery.builder()
                    .ownerId(userId)
                    .lastPulledSequence(-1L)
                    .build();

            List<SyncCursorQuery> cursors = List.of(validCursor, invalidCursor);

            when(syncChangeRepository.findChangesAfterBatch(List.of(validCursor)))
                    .thenReturn(Map.of());

            Map<UUID, List<SyncChange>> result = syncService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
        }

        @Test
        void givenNonHouseholdMember_shouldThrowForbiddenException() {
            UUID requestingUserId = UUID.randomUUID();
            UUID nonMemberId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(requestingUserId)).thenReturn(List.of(requestingUserId));

            SyncCursorQuery cursor = SyncCursorQuery.builder()
                    .ownerId(nonMemberId)
                    .lastPulledSequence(1L)
                    .build();

            List<SyncCursorQuery> cursors = List.of(cursor);

            assertThatThrownBy(() -> syncService.pullChanges(requestingUserId, 0L, cursors))
                    .isInstanceOf(com.fabiankevin.app.exceptions.party.ForbiddenException.class);
        }

        @Test
        void givenHouseholdMember_shouldPassValidation() {
            UUID requestingUserId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(requestingUserId)).thenReturn(List.of(requestingUserId, memberId));

            SyncCursorQuery cursor = SyncCursorQuery.builder()
                    .ownerId(memberId)
                    .lastPulledSequence(1L)
                    .build();

            when(syncChangeRepository.findChangesAfterBatch(List.of(cursor)))
                    .thenReturn(Map.of());

            Map<UUID, List<SyncChange>> result = syncService.pullChanges(requestingUserId, 0L, List.of(cursor));

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(List.of(cursor));
        }
    }
}
