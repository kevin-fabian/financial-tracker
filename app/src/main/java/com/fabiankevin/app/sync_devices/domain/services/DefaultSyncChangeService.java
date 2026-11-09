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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSyncChangeService implements SyncChangeService {
    private static final int DEFAULT_PAGE_SIZE = 100;

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

        SyncCursorQuery primaryCursor = SyncCursorQuery.builder()
                .ownerId(requestingUserId)
                .lastPulledSequence(lastSequenceId)
                .build();

        List<SyncCursorQuery> allCursors = Stream.concat(
                        Stream.of(primaryCursor),
                        cursors.stream())
                .toList();

        List<SyncCursorQuery> validCursors = allCursors.stream()
                .filter(cursor -> cursor.lastPulledSequence() != null && cursor.lastPulledSequence() >= 0)
                .toList();

        if (validCursors.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<SyncChange>> result = new HashMap<>();

        for (SyncCursorQuery cursor : validCursors) {
            UUID ownerId = cursor.ownerId();
            Long lastPulledSequence = cursor.lastPulledSequence();
            List<SyncChange> accumulatedChanges = new ArrayList<>();

            while (true) {
                List<SyncChange> page = syncChangeRepository.findChangesAfterBatch(
                        ownerId, lastPulledSequence, DEFAULT_PAGE_SIZE);

                if (page.isEmpty()) {
                    break;
                }

                accumulatedChanges.addAll(page);
                lastPulledSequence = page.getLast().sequenceId();

                if (page.size() < DEFAULT_PAGE_SIZE) {
                    break;
                }
            }

            if (!accumulatedChanges.isEmpty()) {
                result.put(ownerId, accumulatedChanges);
            }
        }

        return result;
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
