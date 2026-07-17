package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.shared_space.*;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.shared_space.*;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultSharedSpaceService implements SharedSpaceService {
    private final SharedSpaceRepository spaceRepository;
    private final UserClient userClient;

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
    public List<SharedSpaceSummary> retrieveByUserId(UUID userId) {
        List<SharedSpace> spaces = spaceRepository.retrieveByUserId(userId);

        List<UUID> allParticipantIds = spaces.stream()
                .flatMap(space -> space.participants().stream())
                .map(SpaceParticipant::userId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = allParticipantIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(allParticipantIds).stream()
                        .collect(Collectors.toMap(User::id, Function.identity()));

        return spaces.stream()
                .map(space -> toSummary(space, usersById))
                .toList();
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

    @Transactional
    @Override
    public void deleteSharedSpace(UUID spaceId, UUID requesterId) {
        SharedSpace space = findSpaceOrThrow(spaceId);

        if (!space.ownerUserId().equals(requesterId)) {
            throw new NotSpaceOwnerException();
        }

        spaceRepository.deleteById(spaceId);
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

    private SharedSpaceSummary toSummary(SharedSpace space, Map<UUID, User> usersById) {
        List<SpaceParticipantSummary> participantSummaries = space.participants().stream()
                .map(participant -> {
                    User user = usersById.get(participant.userId());
                    String name = user != null ? user.firstName() + " " + user.lastName() : null;
                    String initial = deriveInitial(user);
                    return SpaceParticipantSummary.builder()
                            .id(participant.userId())
                            .name(name)
                            .initial(initial)
                            .accessLevel(participant.accessLevel())
                            .status(participant.status())
                            .joinedAt(participant.joinedAt())
                            .build();
                })
                .toList();

        return SharedSpaceSummary.builder()
                .id(space.id())
                .spaceName(space.spaceName())
                .ownerUserId(space.ownerUserId())
                .participants(participantSummaries)
                .sharingMode(space.sharingMode())
                .sharedResources(space.sharedResources())
                .active(space.active())
                .createdAt(space.createdAt())
                .updatedAt(space.updatedAt())
                .build();
    }

    private String deriveInitial(User user) {
        if (user == null || user.firstName() == null || user.lastName() == null) {
            return null;
        }
        return "" + user.firstName().charAt(0) + user.lastName().charAt(0);
    }
}
