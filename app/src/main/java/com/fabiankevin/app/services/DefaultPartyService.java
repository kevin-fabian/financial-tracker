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
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultPartyService implements PartyService {
    private final SharedSpaceRepository spaceRepository;
    private final UserClient userClient;

    @Transactional
    @Override
    public Party organize(OrganizePartyCommand command) {
        List<Player> initialParticipants = new ArrayList<>();
        initialParticipants.add(Player.builder()
                .playerId(command.partyLeaderId())
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

        Party newSpace = Party.builder()
                .name(command.partyName() != null ? command.partyName() : "Shared Space")
                .partyLeaderId(command.partyLeaderId())
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
        Party space = findSpaceOrThrow(spaceId);

        boolean isOwner = space.partyLeaderId().equals(requesterId);
        boolean isSelf = participantId.equals(requesterId);

        if (!isOwner && !isSelf) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        if (participantId.equals(space.partyLeaderId())) {
            throw new CannotRemoveOwnerException();
        }

        List<Player> updatedParticipants = space.participants().stream()
                .filter(p -> !p.playerId().equals(participantId))
                .collect(Collectors.toList());

        Party updatedSpace = space.toBuilder()
                .participants(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        spaceRepository.save(updatedSpace);
    }

    @Override
    public List<SharedSpaceSummary> retrieveByUserId(UUID userId) {
        List<Party> spaces = spaceRepository.retrieveByUserId(userId);

        List<UUID> allParticipantIds = spaces.stream()
                .flatMap(space -> space.participants().stream())
                .map(Player::playerId)
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
        Party space = findSpaceOrThrow(spaceId);

        SharedResource resource = SharedResource.builder()
                .type(command.type())
                .items(command.itemIds())
                .sharedAt(Instant.now())
                .build();

        List<SharedResource> updatedResources = new ArrayList<>(space.sharedResources());
        updatedResources.add(resource);

        Party updatedSpace = space.toBuilder()
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
        Party space = findSpaceOrThrow(spaceId);

        if (!space.partyLeaderId().equals(requesterId)) {
            throw new NotSpaceOwnerException();
        }

        spaceRepository.deleteById(spaceId);
    }

    @Transactional
    @Override
    public Party patchSharedSpace(PatchPartyCommand command) {
        Party existing = findSpaceOrThrow(command.id());

        if (!existing.partyLeaderId().equals(command.userId())) {
            throw new NotSpaceOwnerException();
        }

        Party.PartyBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(command.partyName())
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);
        Optional.ofNullable(command.sharingMode())
                .ifPresent(builder::sharingMode);

        return spaceRepository.save(builder.build());
    }

    private Party findSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private Player findParticipantOrThrow(Party space, UUID userId) {
        return space.participants().stream()
                .filter(p -> p.playerId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ParticipantNotFoundException(userId));
    }

    private SharedSpaceSummary toSummary(Party space, Map<UUID, User> usersById) {
        List<SpaceParticipantSummary> participantSummaries = space.participants().stream()
                .map(participant -> {
                    User user = usersById.get(participant.playerId());
                    String name = user != null ? user.firstName() + " " + user.lastName() : null;
                    String initial = deriveInitial(user);
                    return SpaceParticipantSummary.builder()
                            .id(participant.playerId())
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
                .spaceName(space.name())
                .ownerUserId(space.partyLeaderId())
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
