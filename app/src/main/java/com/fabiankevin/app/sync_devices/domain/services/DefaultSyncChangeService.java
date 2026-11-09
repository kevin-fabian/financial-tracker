package com.fabiankevin.app.sync_devices.domain.services;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.sync_devices.domain.services.queries.SyncCursorQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSyncChangeService implements SyncChangeService {
    private final HouseholdService householdService;
    private final SyncChangeRepository syncChangeRepository;

    @Override
    public List<SyncScope> pullChanges(UUID requestingUserId, long lastSequenceId, List<SyncCursorQuery> cursors) {
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
            return List.of();
        }

        PageQuery pageQuery = PageQuery.withDefaults();

        List<SyncScope> scopes = new ArrayList<>();

        for (SyncCursorQuery cursor : validCursors) {
            UUID ownerId = cursor.ownerId();

            var page = syncChangeRepository.findChangesAfterBatch(
                    ownerId, cursor.lastPulledSequence(), pageQuery);

            if (page.content().isEmpty()) {
                continue;
            }

            Long nextCursor = page.content().stream()
                    .mapToLong(SyncChange::sequenceId)
                    .max()
                    .orElse(cursor.lastPulledSequence());

            boolean hasMore = !page.last();

            scopes.add(SyncScope.builder()
                    .userId(ownerId)
                    .changes(page.content())
                    .nextCursor(nextCursor)
                    .hasMore(hasMore)
                    .build());
        }

        return scopes;
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
