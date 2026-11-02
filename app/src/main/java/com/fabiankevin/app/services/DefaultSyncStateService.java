package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.SyncStateNotFoundException;
import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.persistence.SyncStateRepository;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.services.commands.PatchSyncStateCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DefaultSyncStateService implements SyncStateService {
    private final SyncStateRepository syncStateRepository;

    @Transactional
    @Override
    public SyncState createSyncState(CreateSyncStateCommand command) {
        SyncStatus status = command.syncStatus() != null ? command.syncStatus() : SyncStatus.PENDING;
        SyncState syncState = SyncState.builder()
                .userId(command.userId())
                .userDevice(command.userDevice())
                .resourceType(command.resourceType())
                .syncStatus(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return syncStateRepository.save(syncState);
    }

    @Transactional
    @Override
    public SyncState patchSyncState(PatchSyncStateCommand command) {
        SyncState existing = syncStateRepository.findById(command.id())
                .filter(s -> s.userId().equals(command.userId()))
                .orElseThrow(SyncStateNotFoundException::new);

        SyncState updated = existing.toBuilder()
                .syncStatus(command.syncStatus())
                .updatedAt(Instant.now())
                .build();
        return syncStateRepository.save(updated);
    }

    @Override
    public SyncState getSyncStateById(UUID id, UUID userId) {
        return syncStateRepository.findById(id)
                .filter(s -> s.userId().equals(userId))
                .orElseThrow(SyncStateNotFoundException::new);
    }

    @Override
    public List<SyncState> getAllSyncStatesByUserId(UUID userId) {
        return syncStateRepository.findAllByUserId(userId);
    }
}
