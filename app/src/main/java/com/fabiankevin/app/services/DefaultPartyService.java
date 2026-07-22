package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.NotPartyLeaderException;
import com.fabiankevin.app.exceptions.party.PartyNotFoundException;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.enums.party.ResourceType;
import com.fabiankevin.app.models.party.*;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.services.commands.party.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;
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
    private final PartyRepository partyRepository;
    private final InvitationRepository invitationRepository;
    private final UserClient userClient;

    private static final String DEFAULT_PARTY_NAME = "New Party";

    @Transactional
    @Override
    public PartySummary organize(OrganizePartyCommand command) {
        Optional<Party> existingParty = partyRepository.findByPlayerId(command.partyLeaderId());
        if (existingParty.isPresent()) {
            return toSummaryWithUsers(existingParty.get());
        }

        List<PartyMember> initialPartyMembers = new ArrayList<>();
        initialPartyMembers.add(PartyMember.builder()
                .playerId(command.partyLeaderId())
                .accessLevel(AccessLevel.READ_WRITE)
                .status(PartyMemberStatus.ACTIVE)
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
                .type(ResourceType.CHECKLIST)
                .sharedAt(Instant.now())
                .items(List.of())
                .build());

        cancelActiveIncomingInvitations(command.partyLeaderId());

        Party newParty = Party.builder()
                .name(command.partyName() != null ? command.partyName() : DEFAULT_PARTY_NAME)
                .partyLeaderId(command.partyLeaderId())
                .partyMembers(initialPartyMembers)
                .sharingMode(command.sharingMode())
                .sharedItems(sharedItems)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Party saved = partyRepository.save(newParty);
        return toSummaryWithUsers(saved);
    }

    @Transactional
    @Override
    public void kickPartyMember(UUID partyId, UUID partyMemberId, UUID requesterId) {
        Party party = findPartyOrThrow(partyId);

        boolean partyLeader = party.partyLeaderId().equals(requesterId);
        boolean isSelf = partyMemberId.equals(requesterId);

        if (!partyLeader && !isSelf) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        if (partyMemberId.equals(party.partyLeaderId())) {
            throw new CannotRemoveOwnerException();
        }

        List<PartyMember> updatedParticipants = party.partyMembers().stream()
                .filter(p -> !p.playerId().equals(partyMemberId))
                .toList();

        Party updatedParty = party.toBuilder()
                .partyMembers(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        partyRepository.save(updatedParty);
    }

    @Override
    public List<PartySummary> retrieveByUserId(UUID userId) {
        return partyRepository.retrieveByPlayerId(userId).stream()
                .map(this::toSummaryWithUsers)
                .toList();
    }

    @Override
    public List<UUID> getPartyMembersUserId(UUID userId) {
        return partyRepository.findPartyMembersPlayerIdsByPlayerId(userId);
    }

    @Transactional
    @Override
    public void disbandParty(UUID partyId, UUID requesterId) {
        Party party = findPartyOrThrow(partyId);

        if (!party.partyLeaderId().equals(requesterId)) {
            throw new NotPartyLeaderException();
        }

        partyRepository.deleteById(partyId);
    }

    @Transactional
    @Override
    public Party patchParty(PatchPartyCommand command) {
        Party existing = findPartyOrThrow(command.id());

        if (!existing.partyLeaderId().equals(command.playerId())) {
            throw new NotPartyLeaderException();
        }

        Party.PartyBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(command.partyName())
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);
        Optional.ofNullable(command.sharingMode())
                .ifPresent(builder::sharingMode);

        return partyRepository.save(builder.build());
    }

    private void cancelActiveIncomingInvitations(UUID userId) {
        invitationRepository.findByInviteeUserId(userId).stream()
                .map(invitation -> Invitation.builder()
                        .id(invitation.id())
                        .inviterPlayerId(invitation.inviterPlayerId())
                        .inviteePlayerId(invitation.inviteePlayerId())
                        .proposedSharingMode(invitation.proposedSharingMode())
                        .proposedRole(invitation.proposedRole())
                        .status(InvitationStatus.CANCELLED)
                        .createdAt(invitation.createdAt())
                        .expiresAt(invitation.expiresAt())
                        .sharedSpaceId(invitation.sharedSpaceId())
                        .build())
                .forEach(invitationRepository::save);
    }

    private Party findPartyOrThrow(UUID partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private PartySummary toSummaryWithUsers(Party party) {
        List<UUID> partyMemberIds = party.partyMembers().stream()
                .map(PartyMember::playerId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = partyMemberIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(partyMemberIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return toSummary(party, usersById);
    }

    private PartySummary toSummary(Party party, Map<UUID, User> playerIds) {
        List<PartyMemberSummary> partyMemberSummaries = party.partyMembers().stream()
                .map(partyMember -> {
                    User user = playerIds.get(partyMember.playerId());
                    String name = user != null ? user.firstName() + " " + user.lastName() : null;
                    String initial = deriveInitial(user);
                    boolean leader = party.partyLeaderId().equals(partyMember.playerId());
                    return PartyMemberSummary.builder()
                            .id(partyMember.id())
                            .playerId(partyMember.playerId())
                            .name(name)
                            .initial(initial)
                            .partyLeader(leader)
                            .partyMember(!leader)
                            .accessLevel(partyMember.accessLevel())
                            .status(partyMember.status())
                            .joinedAt(partyMember.joinedAt())
                            .build();
                })
                .toList();

        return PartySummary.builder()
                .id(party.id())
                .name(party.name())
                .partyLeaderId(party.partyLeaderId())
                .partyMembers(partyMemberSummaries)
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
