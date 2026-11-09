package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultSyncChangeServiceTest {

    private final HouseholdService householdService = mock(HouseholdService.class);
    private final SyncChangeRepository syncChangeRepository = mock(SyncChangeRepository.class);

    private final DefaultSyncChangeService syncChangeService = new DefaultSyncChangeService(
            householdService,
            syncChangeRepository
    );

    @Nested
    class PullChanges {

        @Test
        void givenEmptyCursors_shouldReturnEmptyMapWithoutCallingRepository() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<SyncCursorQuery> cursors = List.of();

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(100));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(100));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(100));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(100));
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(1L), eq(100));
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

            when(syncChangeRepository.findChangesAfterBatch(eq(requestingUserId), eq(0L), eq(100)))
                    .thenReturn(List.of());
            when(syncChangeRepository.findChangesAfterBatch(eq(memberId), eq(1L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(requestingUserId, 0L, List.of(cursor));

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(requestingUserId), eq(0L), eq(100));
            verify(syncChangeRepository).findChangesAfterBatch(eq(memberId), eq(1L), eq(100));
        }

        @Test
        void givenValidCursors_shouldReturnChangesFromRepository() {
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());
            when(syncChangeRepository.findChangesAfterBatch(eq(changeUserId), eq(100L), eq(100)))
                    .thenReturn(List.of(change));

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, List.of(cursor));

            assertThat(result).containsKey(changeUserId);
            assertThat(result.get(changeUserId)).hasSize(1);
            assertThat(result.get(changeUserId).getFirst().sequenceId()).isEqualTo(101L);
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
        void givenAllCursorsInvalid_shouldReturnEmptyMapWithoutCallingRepository() {
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

            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(List.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(eq(userId), eq(0L), eq(100));
        }

        @Test
        void givenLargeNumberOfChanges_shouldPaginateThroughAllChanges() {
            UUID userId = UUID.randomUUID();
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            // Build a page of exactly DEFAULT_PAGE_SIZE (100) items to trigger next page
            List<SyncChange> page1 = java.util.stream.IntStream.rangeClosed(1, 100)
                    .mapToObj(i -> SyncChange.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .sequenceId((long) i)
                            .entityType(com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS)
                            .entityId(UUID.randomUUID())
                            .operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE)
                            .payload("{}")
                            .createdAt(Instant.now())
                            .build())
                    .toList();

            // Second page returns fewer items (pagination complete)
            List<SyncChange> page2 = List.of(
                    SyncChange.builder().id(UUID.randomUUID()).userId(userId).sequenceId(101L).entityType(com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS).entityId(UUID.randomUUID()).operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE).payload("{}").createdAt(Instant.now()).build(),
                    SyncChange.builder().id(UUID.randomUUID()).userId(userId).sequenceId(102L).entityType(com.fabiankevin.app.models.enums.ResourceType.TRANSACTIONS).entityId(UUID.randomUUID()).operation(com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType.CREATE).payload("{}").createdAt(Instant.now()).build()
            );

            // First call returns a full page (triggers next page)
            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(0L), eq(100)))
                    .thenReturn(page1);
            // Second call returns partial page (pagination complete)
            when(syncChangeRepository.findChangesAfterBatch(eq(userId), eq(100L), eq(100)))
                    .thenReturn(page2);

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, List.of());

            assertThat(result).containsKey(userId);
            assertThat(result.get(userId)).hasSize(102);
            assertThat(result.get(userId))
                    .extracting(SyncChange::sequenceId)
                    .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                            11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
                            21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,
                            31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L,
                            41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L,
                            51L, 52L, 53L, 54L, 55L, 56L, 57L, 58L, 59L, 60L,
                            61L, 62L, 63L, 64L, 65L, 66L, 67L, 68L, 69L, 70L,
                            71L, 72L, 73L, 74L, 75L, 76L, 77L, 78L, 79L, 80L,
                            81L, 82L, 83L, 84L, 85L, 86L, 87L, 88L, 89L, 90L,
                            91L, 92L, 93L, 94L, 95L, 96L, 97L, 98L, 99L, 100L,
                            101L, 102L);
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
}
