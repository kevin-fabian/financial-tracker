package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.ForbiddenException;
import com.fabiankevin.app.exceptions.shared_space.ParticipantNotFoundException;
import com.fabiankevin.app.exceptions.shared_space.SharedSpaceNotFoundException;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultSharedSpaceService implements SharedSpaceService {
    private final SharedSpaceRepository spaceRepository;

    @Transactional
    @Override
    public SharedSpace createShare(CreateSharedSpaceCommand command) {
        List<SpaceParticipant> initialParticipants = new ArrayList<>();
        initialParticipants.add(SpaceParticipant.builder()
                .userId(command.ownerUserId())
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        List<SharedResource> sharedResources = new ArrayList<>();
        sharedResources.add(SharedResource.builder()
                .type(ResourceType.TRANSACTION)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());
        sharedResources.add(SharedResource.builder()
                .type(ResourceType.BUDGET)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());
        sharedResources.add(SharedResource.builder()
                .type(ResourceType.BUDGET)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());

        SharedSpace newSpace = SharedSpace.builder()
                .spaceName(command.spaceName() != null ? command.spaceName() : "Shared Space")
                .ownerUserId(command.ownerUserId())
                .participants(initialParticipants)
                .sharingMode(command.sharingMode())
                .sharedResources(sharedResources)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return spaceRepository.save(newSpace);
    }

    @Transactional
    @Override
    public void removeParticipant(UUID spaceId, UUID participantId, UUID requesterId) {
        SharedSpace space = findSpaceOrThrow(spaceId);

        boolean isOwner = space.ownerUserId().equals(requesterId);
        boolean isSelf = participantId.equals(requesterId);

        if (!isOwner && !isSelf) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        if (participantId.equals(space.ownerUserId())) {
            throw new CannotRemoveOwnerException();
        }

        List<SpaceParticipant> updatedParticipants = space.participants().stream()
                .filter(p -> !p.userId().equals(participantId))
                .collect(Collectors.toList());

        SharedSpace updatedSpace = space.toBuilder()
                .participants(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        spaceRepository.save(updatedSpace);
    }

    @Override
    public List<SharedSpace> retrieveByUserId(UUID userId) {
        return spaceRepository.retrieveByUserId(userId);
    }

    @Transactional
    @Override
    public SharedResource addResource(UUID spaceId, AddSharedResourceCommand command) {
        SharedSpace space = findSpaceOrThrow(spaceId);

        SharedResource resource = SharedResource.builder()
                .type(command.type())
                .items(command.itemIds())
                .sharedAt(Instant.now())
                .build();

        List<SharedResource> updatedResources = new ArrayList<>(space.sharedResources());
        updatedResources.add(resource);

        SharedSpace updatedSpace = space.toBuilder()
                .sharedResources(updatedResources)
                .updatedAt(Instant.now())
                .build();

        spaceRepository.save(updatedSpace);
        return resource;
    }

    @Override
    public List<UUID> getParticipantUserIds(UUID userId) {
        return spaceRepository.findParticipantUserIdsByUserId(userId);
    }

    private SharedSpace findSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private SpaceParticipant findParticipantOrThrow(SharedSpace space, UUID userId) {
        return space.participants().stream()
                .filter(p -> p.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ParticipantNotFoundException(userId));
    }
}
