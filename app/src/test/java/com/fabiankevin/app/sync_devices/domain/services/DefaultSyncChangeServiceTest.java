package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultSyncChangeServiceTest {

    private static final PageQuery DEFAULT_PAGE_QUERY = PageQuery.withDefaults();

    private final HouseholdService householdService = mock(HouseholdService.class);
    private final SyncChangeRepository syncChangeRepository = mock(SyncChangeRepository.class);

    private final DefaultSyncChangeService syncChangeService = new DefaultSyncChangeService(
            householdService,
            syncChangeRepository
    );

    @Nested
    class PullChanges {

        @Test
        void givenEmptyCursors_shouldReturnEmptyListWithoutCallingRepository() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<SyncCursorQuery> cursors = List.of();

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY));
        }

        @Test
        void givenMixedValidAndInvalidCursors_shouldOnlyProcessValidCursors() {
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));
            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(1L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY));
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(1L), eq(DEFAULT_PAGE_QUERY));
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

            assertThatThrownBy(() -> syncChangeService.pullChanges(requestingUserId, 0L, cursors))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("User is not a member of the household");

            verifyNoInteractions(syncChangeRepository);
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

            when(syncChangeRepository.findChangesAfterBatch(eq(requestingUserId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));
            when(syncChangeRepository.findChangesAfterBatch(eq(memberId), eq(1L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(requestingUserId, 0L, List.of(cursor));

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(requestingUserId), eq(0L), eq(DEFAULT_PAGE_QUERY));
            verify(syncChangeRepository).findChangesAfterBatch(eq(memberId), eq(1L), eq(DEFAULT_PAGE_QUERY));
        }

        @Test
        void givenValidCursors_shouldReturnScopesWithChangesFromRepository() {
            UUID userId = UUID.randomUUID();
            UUID changeUserId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId, changeUserId));

            SyncCursorQuery cursor = SyncCursorQuery.builder()
                    .ownerId(changeUserId)
                    .lastPulledSequence(100L)
                    .build();

            SyncChange change = SyncChange.builder()
                    .id(UUID.randomUUID())
                    .userId(changeUserId)
                    .sequenceId(101L)
                    .entityType(com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS)
                    .entityId(UUID.randomUUID())
                    .operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));
            when(syncChangeRepository.findChangesAfterBatch(eq(changeUserId), eq(100L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(change), 0, 10, 1, 1, true, false));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, List.of(cursor));

            assertThat(result).hasSize(1);
            SyncScope scope = result.getFirst();
            assertThat(scope.userId()).isEqualTo(changeUserId);
            assertThat(scope.changes()).hasSize(1);
            assertThat(scope.changes().getFirst().sequenceId()).isEqualTo(101L);
            assertThat(scope.nextCursor()).isEqualTo(101L);
            assertThat(scope.hasMore()).isFalse();
        }

        @Test
        void givenMultipleHouseholdMembers_shouldValidateAllCursors() {
            UUID requestingUserId = UUID.randomUUID();
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            UUID nonMemberId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(requestingUserId)).thenReturn(List.of(requestingUserId, memberId1, memberId2));

            SyncCursorQuery validCursor1 = SyncCursorQuery.builder()
                    .ownerId(memberId1)
                    .lastPulledSequence(1L)
                    .build();

            SyncCursorQuery validCursor2 = SyncCursorQuery.builder()
                    .ownerId(memberId2)
                    .lastPulledSequence(2L)
                    .build();

            SyncCursorQuery invalidCursor = SyncCursorQuery.builder()
                    .ownerId(nonMemberId)
                    .lastPulledSequence(3L)
                    .build();

            List<SyncCursorQuery> cursors = List.of(validCursor1, validCursor2, invalidCursor);

            assertThatThrownBy(() -> syncChangeService.pullChanges(requestingUserId, 0L, cursors))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("User is not a member of the household");

            verifyNoInteractions(syncChangeRepository);
        }

        @Test
        void givenAllCursorsInvalid_shouldReturnEmptyListWithoutCallingRepository() {
            UUID userId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId, memberId));

            SyncCursorQuery invalidCursor1 = SyncCursorQuery.builder()
                    .ownerId(memberId)
                    .lastPulledSequence(-1L)
                    .build();

            SyncCursorQuery invalidCursor2 = SyncCursorQuery.builder()
                    .ownerId(memberId)
                    .lastPulledSequence(null)
                    .build();

            List<SyncCursorQuery> cursors = List.of(invalidCursor1, invalidCursor2);

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY));
        }

        @Test
        void givenMoreChangesThanPageSize_shouldReturnFirstPageWithHasMoreTrue() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<SyncChange> page = java.util.stream.IntStream.rangeClosed(1, 10)
                    .mapToObj(i -> SyncChange.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .sequenceId(i)
                            .entityType(com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS)
                            .entityId(UUID.randomUUID())
                            .operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE)
                            .payload("{}")
                            .createdAt(Instant.now())
                            .build())
                    .toList();

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(DEFAULT_PAGE_QUERY)))
                    .thenReturn(new Page<>(page, 0, 10, 15, 2, false, true));

            List<SyncScope> result = syncChangeService.pullChanges(userId, 0L, List.of());

            assertThat(result).hasSize(1);
            SyncScope scope = result.getFirst();
            assertThat(scope.userId()).isEqualTo(userId);
            assertThat(scope.changes()).hasSize(10);
            assertThat(scope.hasMore()).isTrue();
            assertThat(scope.nextCursor()).isEqualTo(10L);
        }
    }

    @Nested
    class Create {

        @Test
        void givenValidCommand_shouldCreateSyncChangeWithSequenceIdOne() {
            UUID userId = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();
            com.fabiankevin.app.models.enums.ResourceType entityType = com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS;
            com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType operation = com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE;
            String payload = "{\"amount\": 100.0}";

            CreateSyncChangeCommand command = CreateSyncChangeCommand.builder()
                    .userId(userId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .operation(operation)
                    .payload(payload)
                    .build();

            when(syncChangeRepository.findMaxSequenceId(userId)).thenReturn(Optional.empty());

            SyncChange expectedChange = SyncChange.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .sequenceId(1L)
                    .entityType(entityType)
                    .entityId(entityId)
                    .operation(operation)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();

            when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(expectedChange);

            SyncChange result = syncChangeService.create(command);

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.sequenceId()).isEqualTo(1L);
            assertThat(result.entityType()).isEqualTo(entityType);
            assertThat(result.entityId()).isEqualTo(entityId);
            assertThat(result.operation()).isEqualTo(operation);
            assertThat(result.payload()).isEqualTo(payload);
            assertThat(result.createdAt()).isNotNull();
            verify(syncChangeRepository).save(any(SyncChange.class));
        }

        @Test
        void givenExistingChanges_shouldCreateSyncChangeWithIncrementedSequenceId() {
            UUID userId = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();
            com.fabiankevin.app.models.enums.ResourceType entityType = com.fabiankevin.app.models.enums.ResourceType.CATEGORIES;
            com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType operation = com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.UPDATE;
            String payload = "{\"name\": \"updated\"}";

            CreateSyncChangeCommand command = CreateSyncChangeCommand.builder()
                    .userId(userId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .operation(operation)
                    .payload(payload)
                    .build();

            when(syncChangeRepository.findMaxSequenceId(userId)).thenReturn(Optional.of(5L));

            SyncChange expectedChange = SyncChange.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .sequenceId(6L)
                    .entityType(entityType)
                    .entityId(entityId)
                    .operation(operation)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();

            when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(expectedChange);

            SyncChange result = syncChangeService.create(command);

            assertThat(result).isNotNull();
            assertThat(result.sequenceId()).isEqualTo(6L);
            verify(syncChangeRepository).save(any(SyncChange.class));
        }

        @Test
        void givenDeleteOperation_shouldCreateSyncChangeWithDeleteOperation() {
            UUID userId = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            CreateSyncChangeCommand command = CreateSyncChangeCommand.builder()
                    .userId(userId)
                    .entityType(com.fabiankevin.app.models.enums.ResourceType.ACCOUNTS)
                    .entityId(entityId)
                    .operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.DELETE)
                    .payload("{}")
                    .build();

            when(syncChangeRepository.findMaxSequenceId(userId)).thenReturn(Optional.empty());

            SyncChange expectedChange = SyncChange.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .sequenceId(1L)
                    .entityType(com.fabiankevin.app.models.enums.ResourceType.ACCOUNTS)
                    .entityId(entityId)
                    .operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.DELETE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(expectedChange);

            SyncChange result = syncChangeService.create(command);

            assertThat(result.operation()).isEqualTo(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.DELETE);
            verify(syncChangeRepository).save(any(SyncChange.class));
        }
    }

    @Nested
    class GetLatestCursor {

        @Test
        void givenUserWithSyncChanges_shouldReturnMaxSequenceId() {
            UUID userId = UUID.randomUUID();
            Long expectedCursor = 150L;

            when(syncChangeRepository.findMaxSequenceId(userId)).thenReturn(Optional.of(expectedCursor));

            Optional<Long> result = syncChangeService.getLatestCursor(userId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedCursor);
            verify(syncChangeRepository).findMaxSequenceId(userId);
        }

        @Test
        void givenUserWithNoSyncChanges_shouldReturnEmpty() {
            UUID userId = UUID.randomUUID();

            when(syncChangeRepository.findMaxSequenceId(userId)).thenReturn(Optional.empty());

            Optional<Long> result = syncChangeService.getLatestCursor(userId);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findMaxSequenceId(userId);
        }
    }
}
