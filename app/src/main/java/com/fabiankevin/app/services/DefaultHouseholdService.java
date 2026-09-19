package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.dtos.HouseholdEventPayload;
import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.HouseholdAlreadyExistsException;
import com.fabiankevin.app.exceptions.party.HouseholdNotFoundException;
import com.fabiankevin.app.exceptions.party.NotHouseholdLeaderException;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.EventAction;
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
import com.fabiankevin.app.services.commands.household.OrganizeHouseholdCommand;
import com.fabiankevin.app.services.commands.household.PatchHouseholdCommand;
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
    private final EventPublisher householdEventPublisher;

    private static final String DEFAULT_HOUSEHOLD_NAME = "New Household";
    private static final int MAX_HOUSEHOLD_NAME_LENGTH = 100;

    @Transactional
    @Override
    public HouseholdSummary organize(OrganizeHouseholdCommand command) {
        if (command.householdName() != null && command.householdName().length() > MAX_HOUSEHOLD_NAME_LENGTH) {
            throw new IllegalArgumentException("Household name must not exceed " + MAX_HOUSEHOLD_NAME_LENGTH + " characters");
        }

        Optional<Household> existingHousehold = householdRepository.findByUserId(command.leaderId());
        if (existingHousehold.isPresent()) {
            throw new HouseholdAlreadyExistsException();
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
                .name((command.householdName() == null || command.householdName().isBlank())
                        ? DEFAULT_HOUSEHOLD_NAME
                        : command.householdName())
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
    public void removeMember(UUID householdId, UUID householdMemberId, UUID requestingUserId) {
        Household household = findHouseholdOrThrow(householdId);

        boolean isHouseholdLeader = household.leaderId().equals(requestingUserId);

        // Find the member being removed and check if they are removing themselves
        Optional<HouseholdMember> memberBeingRemoved = household.members().stream()
                .filter(m -> m.id().equals(householdMemberId))
                .findFirst();

        boolean isMemberThemselves = memberBeingRemoved
                .map(m -> m.userId().equals(requestingUserId))
                .orElse(false);

        if (!isHouseholdLeader && !isMemberThemselves) {
            throw new ForbiddenException("Only the owner or the participant themselves can remove a participant");
        }

        // Get the userId of the member being removed to check if they are the owner
        UUID memberId = memberBeingRemoved
                .map(HouseholdMember::userId)
                .orElse(householdMemberId);

        if (memberId.equals(household.leaderId())) {
            throw new CannotRemoveOwnerException();
        }

        List<HouseholdMember> updatedMembers = household.members().stream()
                .filter(m -> !m.id().equals(householdMemberId))
                .toList();

        Household updatedHousehold = household.toBuilder()
                .members(updatedMembers)
                .updatedAt(Instant.now())
                .build();

        Household saved = householdRepository.save(updatedHousehold);
        HouseholdSummary summary = toSummary(saved, Map.of(), Map.of());
        householdEventPublisher.publish(household.id(), new HouseholdEventPayload(
                EventAction.LEAVE_HOUSEHOLD,
                summary
        ));
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
    public void disbandHousehold(UUID householdId, UUID leaderId) {
        Household household = findHouseholdOrThrow(householdId);

        if (!household.leaderId().equals(leaderId)) {
            throw new NotHouseholdLeaderException();
        }

        householdRepository.deleteById(householdId);
    }

    @Transactional
    @Override
    public Household patchHousehold(PatchHouseholdCommand command) {
        Household existing = findHouseholdOrThrow(command.id());

        if (!existing.leaderId().equals(command.playerId())) {
            throw new NotHouseholdLeaderException();
        }

        Household.HouseholdBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(command.householdName())
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);

        return householdRepository.save(builder.build());
    }

    private void cancelActiveIncomingInvitations(UUID userId) {
        invitationRepository.findByInviteeUserId(userId).stream()
                .map(invitation -> Invitation.builder()
                        .id(invitation.id())
                        .inviterUserId(invitation.inviterUserId())
                        .inviteeUserId(invitation.inviteeUserId())
                        .proposedRole(invitation.proposedRole())
                        .status(InvitationStatus.CANCELLED)
                        .createdAt(invitation.createdAt())
                        .expiresAt(invitation.expiresAt())
                        .householdId(invitation.householdId())
                        .build())
                .forEach(invitationRepository::save);
    }

    private Household findHouseholdOrThrow(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(HouseholdNotFoundException::new);
    }

    private HouseholdSummary toSummaryWithUsers(Household household) {
        List<UUID> householdMemberIds = household.members().stream()
                .map(HouseholdMember::userId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = householdMemberIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(householdMemberIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        Map<UUID, Double> dailyAverageByUserId = householdMemberIds.isEmpty()
                ? Map.of()
                : transactionRepository.getDailyAveragePastWeek(new HashSet<>(householdMemberIds)).stream()
                .collect(Collectors.toMap(sp -> UUID.fromString(sp.label()), SummaryPoint::total));

        return toSummary(household, usersById, dailyAverageByUserId);
    }

    private HouseholdSummary toSummary(Household household, Map<UUID, User> usersById, Map<UUID, Double> dailyAverageByUserId) {
        List<HouseholdMemberSummary> householdMemberSummaries = household.members().stream()
                .map(householdMember -> {
                    User user = usersById.get(householdMember.userId());
                    boolean leader = household.leaderId().equals(householdMember.userId());
                    return HouseholdMemberSummary.builder()
                            .id(householdMember.id())
                            .user(user)
                            .householdLeader(leader)
                            .status(householdMember.status())
                            .joinedAt(householdMember.joinedAt())
                            .build();
                })
                .toList();

        return HouseholdSummary.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .members(householdMemberSummaries)
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();
    }
}
