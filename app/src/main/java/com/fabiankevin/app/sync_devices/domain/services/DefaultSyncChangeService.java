package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSyncChangeService implements SyncChangeService {
    private final HouseholdService householdService;
    private final SyncChangeRepository syncChangeRepository;

    @Override
    public Map<UUID, List<SyncChange>> pullChanges(UUID requestingUserId, long lastSequenceId, List<SyncCursorQuery> cursors) {
        List<UUID> householdMemberIds = householdService.getHouseholdMembersUserIds(requestingUserId);

        for (SyncCursorQuery cursor : cursors) {
            if (!householdMemberIds.contains(cursor.ownerId())) {
                throw new ForbiddenException(
                        "User is not a member of the household");
            }
        }

        List<SyncCursorQuery> validCursors = cursors.stream()
                .filter(cursor -> cursor.lastPulledSequence() != null && cursor.lastPulledSequence() >= 0)
                .toList();

        if (validCursors.isEmpty()) {
            return Map.of();
        }

        return syncChangeRepository.findChangesAfterBatch(validCursors);
    }

    @Override
    public SyncChange create(CreateSyncChangeCommand command) {
        long nextSequenceId = getNextSequenceId(command.userId());
        SyncChange syncChange = SyncChange.builder()
                .userId(command.userId())
                .sequenceId(nextSequenceId)
                .entityType(command.entityType())
                .entityId(command.entityId())
                .operation(command.operation())
                .payload(command.payload())
                .createdAt(Instant.now())
                .build();

        return syncChangeRepository.save(syncChange);
    }

    private long getNextSequenceId(UUID userId) {
        return syncChangeRepository.findMaxSequenceId(userId)
                .map(max -> max + 1)
                .orElse(1L);
    }
}
