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
        List<PartyMember> initialParticipants = new ArrayList<>();
        initialParticipants.add(PartyMember.builder()
                .playerId(command.partyLeaderId())
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        List<SharedItem> sharedItems = new ArrayList<>();
        sharedItems.add(SharedItem.builder()
                .type(ResourceType.TRANSACTION)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());
        sharedItems.add(SharedItem.builder()
                .type(ResourceType.BUDGET)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());
        sharedItems.add(SharedItem.builder()
                .type(ResourceType.BUDGET)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());

        Party newSpace = Party.builder()
                .name(command.partyName() != null ? command.partyName() : "Shared Space")
                .partyLeaderId(command.partyLeaderId())
                .partyMembers(initialParticipants)
                .sharingMode(command.sharingMode())
                .sharedItems(sharedItems)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return spaceRepository.save(newSpace);
    }

    @Transactional
    @Override
    public void removeParticipant(UUID partyId, UUID participantId, UUID requesterId) {
        Party party = findPartyOrThrow(partyId);

        boolean isOwner = party.partyLeaderId().equals(requesterId);
        boolean isSelf = participantId.equals(requesterId);

        if (!isOwner && !isSelf) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        if (participantId.equals(party.partyLeaderId())) {
            throw new CannotRemoveOwnerException();
        }

        List<PartyMember> updatedParticipants = party.partyMembers().stream()
                .filter(p -> !p.playerId().equals(participantId))
                .collect(Collectors.toList());

        Party updatedParty = party.toBuilder()
                .partyMembers(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        spaceRepository.save(updatedParty);
    }

    @Override
    public List<PartySummary> retrieveByUserId(UUID userId) {
        List<Party> parties = spaceRepository.retrieveByUserId(userId);

        List<UUID> allParticipantIds = parties.stream()
                .flatMap(party -> party.partyMembers().stream())
                .map(PartyMember::playerId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = allParticipantIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(allParticipantIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return parties.stream()
                .map(party -> toSummary(party, usersById))
                .toList();
    }

    @Transactional
    @Override
    public SharedItem addResource(UUID partyId, AddSharedResourceCommand command) {
        Party party = findPartyOrThrow(partyId);

        SharedItem resource = SharedItem.builder()
                .type(command.type())
                .items(command.itemIds())
                .sharedAt(Instant.now())
                .build();

        List<SharedItem> updatedResources = new ArrayList<>(party.sharedItems());
        updatedResources.add(resource);

        Party updatedParty = party.toBuilder()
                .sharedItems(updatedResources)
                .updatedAt(Instant.now())
                .build();

        spaceRepository.save(updatedParty);
        return resource;
    }

    @Override
    public List<UUID> getParticipantUserIds(UUID userId) {
        return spaceRepository.findParticipantUserIdsByUserId(userId);
    }

    @Transactional
    @Override
    public void deleteParty(UUID partyId, UUID requesterId) {
        Party party = findPartyOrThrow(partyId);

        if (!party.partyLeaderId().equals(requesterId)) {
            throw new NotSpaceOwnerException();
        }

        spaceRepository.deleteById(partyId);
    }

    @Transactional
    @Override
    public Party patchParty(PatchPartyCommand command) {
        Party existing = findPartyOrThrow(command.id());

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

    private Party findPartyOrThrow(UUID partyId) {
        return spaceRepository.findById(partyId)
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private PartyMember findParticipantOrThrow(Party party, UUID userId) {
        return party.partyMembers().stream()
                .filter(p -> p.playerId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ParticipantNotFoundException(userId));
    }

    private PartySummary toSummary(Party party, Map<UUID, User> usersById) {
        List<PartyMemberSummary> participantSummaries = party.partyMembers().stream()
                .map(participant -> {
                    User user = usersById.get(participant.playerId());
                    String name = user != null ? user.firstName() + " " + user.lastName() : null;
                    String initial = deriveInitial(user);
                    return PartyMemberSummary.builder()
                            .id(participant.playerId())
                            .name(name)
                            .initial(initial)
                            .accessLevel(participant.accessLevel())
                            .status(participant.status())
                            .joinedAt(participant.joinedAt())
                            .build();
                })
                .toList();

        return PartySummary.builder()
                .id(party.id())
                .name(party.name())
                .partyLeaderId(party.partyLeaderId())
                .participants(participantSummaries)
                .sharingMode(party.sharingMode())
                .sharedItems(party.sharedItems())
                .active(party.active())
                .createdAt(party.createdAt())
                .updatedAt(party.updatedAt())
                .build();
    }

    private String deriveInitial(User user) {
        if (user == null || user.firstName() == null || user.lastName() == null) {
            return null;
        }
        return "" + user.firstName().charAt(0) + user.lastName().charAt(0);
    }
}
