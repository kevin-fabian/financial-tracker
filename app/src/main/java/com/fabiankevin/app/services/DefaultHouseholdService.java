package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.NotPartyLeaderException;
import com.fabiankevin.app.exceptions.party.PartyNotFoundException;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdMember;
import com.fabiankevin.app.models.household.HouseholdMemberSummary;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.models.household.Invitation;
import com.fabiankevin.app.persistence.HouseholdRepository;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultHouseholdService implements HouseholdService {
    private final HouseholdRepository householdRepository;
    private final InvitationRepository invitationRepository;
    private final TransactionRepository transactionRepository;
    private final UserClient userClient;

    private static final String DEFAULT_PARTY_NAME = "New Party";

    @Transactional
    @Override
    public HouseholdSummary organize(OrganizeHouseholdCommand command) {
        Optional<Household> existingParty = householdRepository.findByUserId(command.leaderId());
        if (existingParty.isPresent()) {
            return toSummaryWithUsers(existingParty.get());
        }

        List<HouseholdMember> initialHouseholdMembers = new ArrayList<>();
        initialHouseholdMembers.add(HouseholdMember.builder()
                .userId(command.leaderId())
                .accessLevel(AccessLevel.VIEW_ONLY)
                .status(HouseholdMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        cancelActiveIncomingInvitations(command.leaderId());

        Household newHousehold = Household.builder()
                .name(command.householdName() != null ? command.householdName() : DEFAULT_PARTY_NAME)
                .leaderId(command.leaderId())
                .members(initialHouseholdMembers)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Household saved = householdRepository.save(newHousehold);
        return toSummaryWithUsers(saved);
    }

    @Transactional
    @Override
    public void removeMember(UUID partyId, UUID partyMemberId, UUID requesterId) {
        Household household = findPartyOrThrow(partyId);

        boolean partyLeader = household.leaderId().equals(requesterId);
        boolean isSelf = partyMemberId.equals(requesterId);

        if (!partyLeader && !isSelf) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        if (partyMemberId.equals(household.leaderId())) {
            throw new CannotRemoveOwnerException();
        }

        List<HouseholdMember> updatedParticipants = household.members().stream()
                .filter(p -> !p.userId().equals(partyMemberId))
                .toList();

        Household updatedHousehold = household.toBuilder()
                .members(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        householdRepository.save(updatedHousehold);
    }

    @Override
    public List<HouseholdSummary> retrieveByUserId(UUID userId) {
        return householdRepository.retrieveByUserId(userId).stream()
                .map(this::toSummaryWithUsers)
                .toList();
    }

    @Override
    public List<UUID> getHouseholdMembersUserIds(UUID userId) {
        List<UUID> memberIds = householdRepository.findMembersUserIdsByUserId(userId);
        return memberIds.isEmpty() ? List.of(userId) : memberIds;
    }

    @Transactional
    @Override
    public void disbandHousehold(UUID householdParty, UUID requesterId) {
        Household household = findPartyOrThrow(householdParty);

        if (!household.leaderId().equals(requesterId)) {
            throw new NotPartyLeaderException();
        }

        householdRepository.deleteById(householdParty);
    }

    @Transactional
    @Override
    public Household patchHousehold(PatchPartyCommand command) {
        Household existing = findPartyOrThrow(command.id());

        if (!existing.leaderId().equals(command.playerId())) {
            throw new NotPartyLeaderException();
        }

        Household.HouseholdBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(command.partyName())
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);

        return householdRepository.save(builder.build());
    }

    private void cancelActiveIncomingInvitations(UUID userId) {
        invitationRepository.findByInviteeUserId(userId).stream()
                .map(invitation -> Invitation.builder()
                        .id(invitation.id())
                        .inviterPlayerId(invitation.inviterPlayerId())
                        .inviteePlayerId(invitation.inviteePlayerId())
                        .proposedRole(invitation.proposedRole())
                        .status(InvitationStatus.CANCELLED)
                        .createdAt(invitation.createdAt())
                        .expiresAt(invitation.expiresAt())
                        .partyId(invitation.partyId())
                        .build())
                .forEach(invitationRepository::save);
    }

    private Household findPartyOrThrow(UUID partyId) {
        return householdRepository.findById(partyId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private HouseholdSummary toSummaryWithUsers(Household household) {
        List<UUID> partyMemberIds = household.members().stream()
                .map(HouseholdMember::userId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = partyMemberIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(partyMemberIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        Map<UUID, Double> dailyAverageByUserId = partyMemberIds.isEmpty()
                ? Map.of()
                : transactionRepository.getDailyAveragePastWeek(new HashSet<>(partyMemberIds)).stream()
                .collect(Collectors.toMap(sp -> UUID.fromString(sp.label()), SummaryPoint::total));

        return toSummary(household, usersById, dailyAverageByUserId);
    }

    private HouseholdSummary toSummary(Household household, Map<UUID, User> playerIds, Map<UUID, Double> dailyAverageByUserId) {
        List<HouseholdMemberSummary> partyMemberSummaries = household.members().stream()
                .map(partyMember -> {
                    User user = playerIds.get(partyMember.userId());
                    String name = user != null ? user.fullName() : null;
                    String initial = deriveInitial(user);
                    boolean leader = household.leaderId().equals(partyMember.userId());
                    return HouseholdMemberSummary.builder()
                            .id(partyMember.id())
                            .playerId(partyMember.userId())
                            .name(name)
                            .initial(initial)
                            .partyLeader(leader)
                            .partyMember(!leader)
                            .accessLevel(partyMember.accessLevel())
                            .status(partyMember.status())
                            .joinedAt(partyMember.joinedAt())
                            .pastWeekDailyAverageTransactionCount(dailyAverageByUserId.getOrDefault(partyMember.userId(), 0.0))
                            .activeShoppingListCount(0)
                            .activeBudgetCount(0)
                            .build();
                })
                .toList();

        return HouseholdSummary.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .members(partyMemberSummaries)
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();
    }

    private String deriveInitial(User user) {
        if (user == null || user.firstName() == null || user.lastName() == null) {
            return null;
        }
        return "" + user.firstName().charAt(0) + user.lastName().charAt(0);
    }
}
