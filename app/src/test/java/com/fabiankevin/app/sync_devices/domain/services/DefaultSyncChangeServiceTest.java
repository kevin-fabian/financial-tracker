package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verifyNoInteractions(syncChangeRepository);
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

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verifyNoInteractions(syncChangeRepository);
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

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verifyNoInteractions(syncChangeRepository);
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

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(List.of(validCursor));
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

            when(syncChangeRepository.findChangesAfterBatch(List.of(cursor)))
                    .thenReturn(Map.of());

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(requestingUserId, 0L, List.of(cursor));

            assertThat(result).isEmpty();
            verify(syncChangeRepository).findChangesAfterBatch(List.of(cursor));
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

            Map<UUID, List<SyncChange>> expectedChanges = Map.of(changeUserId, List.of(change));
            when(syncChangeRepository.findChangesAfterBatch(List.of(cursor)))
                    .thenReturn(expectedChanges);

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, List.of(cursor));

            assertThat(result).isEqualTo(expectedChanges);
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

            Map<UUID, List<SyncChange>> result = syncChangeService.pullChanges(userId, 0L, cursors);

            assertThat(result).isEmpty();
            verifyNoInteractions(syncChangeRepository);
        }
    }
}
