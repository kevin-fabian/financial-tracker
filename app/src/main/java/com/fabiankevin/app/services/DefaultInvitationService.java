package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.events.dtos.InvitationEventPayload;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.HouseholdMemberAlreadyExistsException;
import com.fabiankevin.app.exceptions.party.HouseholdNotFoundException;
import com.fabiankevin.app.exceptions.party.InvitationAlreadyHandledException;
import com.fabiankevin.app.exceptions.party.InvitationExpiredException;
import com.fabiankevin.app.exceptions.party.InvitationNotFoundException;
import com.fabiankevin.app.exceptions.party.InviterCannotAcceptOwnInvitationException;
import com.fabiankevin.app.exceptions.party.NotHouseholdLeaderException;
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
import com.fabiankevin.app.models.household.InvitationSummary;
import com.fabiankevin.app.persistence.HouseholdRepository;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.services.commands.household.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.SendInvitationCommand;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultInvitationService implements InvitationService {
    private final InvitationRepository invitationRepository;
    private final HouseholdRepository householdRepository;
    private final UserClient userClient;
    private final EventPublisher eventPublisher;

    public DefaultInvitationService(
            InvitationRepository invitationRepository,
            HouseholdRepository householdRepository,
            UserClient userClient,
            @Qualifier("invitationEventPublisher")
            EventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.householdRepository = householdRepository;
        this.userClient = userClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public InvitationSummary sendInvitation(SendInvitationCommand command) {
        Household household = findHouseholdOrThrow(command.householdId());
        if (!household.leaderId().equals(command.inviterUserId())) {
            throw new NotHouseholdLeaderException();
        }

        User recipient = userClient.getUserByEmail(command.inviteeEmail());

        if (isUserParticipant(household, recipient.id())) {
            throw new HouseholdMemberAlreadyExistsException();
        }

        if (householdRepository.findByUserId(recipient.id()).isPresent()) {
            throw new HouseholdMemberAlreadyExistsException();
        }

        Invitation invitation = invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(
                        command.householdId(), command.inviterUserId(), recipient.id())
                .orElseGet(() -> {
                    Invitation newInvitation = Invitation.builder()
                            .inviterUserId(command.inviterUserId())
                            .inviteeUserId(recipient.id())
                            .proposedRole(AccessLevel.VIEW_ONLY)
                            .status(InvitationStatus.PENDING)
                            .createdAt(Instant.now())
                            .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                            .householdId(household.id())
                            .build();

                    return invitationRepository.save(newInvitation);
                });

        InvitationSummary summary = toSummary(invitation, command.inviterUserId());
        eventPublisher.publish(recipient.id(), new InvitationEventPayload(
                EventAction.INVITED,
                summary
        ));

        return summary;
    }

    @Transactional
    @Override
    public InvitationSummary acceptInvitation(AcceptInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());
        validateInvitationActive(invitation);

        if (invitation.inviterUserId().equals(command.acceptingUserId())) {
            throw new InviterCannotAcceptOwnInvitationException();
        }

        if (!invitation.inviteeUserId().equals(command.acceptingUserId())) {
            throw new ForbiddenException("Only the invited user can accept");
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeUserId(invitation.inviteeUserId())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.ACCEPTED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .householdId(invitation.householdId())
                .build();
        invitationRepository.save(updatedInvitation);

        Household household = findHouseholdOrThrow(invitation.householdId());

        HouseholdMember participant = HouseholdMember.builder()
                .userId(invitation.inviteeUserId())
                .accessLevel(invitation.proposedRole())
                .status(HouseholdMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        List<HouseholdMember> updatedParticipants = new ArrayList<>(household.members());
        updatedParticipants.add(participant);

        Household updatedSpace = household.toBuilder()
                .members(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        householdRepository.save(updatedSpace);

        InvitationSummary summary = toSummary(updatedInvitation, command.acceptingUserId());
        household.members().stream()
                .filter(member -> member.userId() != command.acceptingUserId())
                .toList()
                .forEach(householdMember -> {
                    eventPublisher.publish(householdMember.userId(), new InvitationEventPayload(
                            EventAction.INVITED,
                            summary
                    ));
                });

        return summary;
    }

    @Transactional
    @Override
    public InvitationSummary rejectInvitation(RejectInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());

        Optional.ofNullable(invitation)
                .filter(inv -> inv.inviteeUserId().equals(command.rejectingUserId()) || inv.inviterUserId().equals(command.rejectingUserId()))
                .orElseThrow(() -> new ForbiddenException("Only the invited user or the inviter can reject the invitation"));

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyHandledException();
        }

        Invitation cancelled = invitation.toBuilder()
                .status(InvitationStatus.CANCELLED)
                .build();
        invitationRepository.save(cancelled);

        return toSummary(cancelled, command.rejectingUserId());
    }

    @Override
    public List<InvitationSummary> getInvitationsByUserId(UUID userId) {
        List<Invitation> invitations = invitationRepository.findByInviterUserIdOrInviteeUserId(userId);

        List<UUID> userIds = invitations.stream()
                .flatMap(invitation -> List.of(invitation.inviterUserId(), invitation.inviteeUserId()).stream())
                .distinct()
                .toList();

        Map<UUID, User> usersById = userIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        List<UUID> householdIds = invitations.stream()
                .map(Invitation::householdId)
                .distinct()
                .toList();

        Map<UUID, Household> householdsById = householdIds.isEmpty()
                ? Map.of()
                : householdRepository.findAllById(householdIds).stream()
                .collect(Collectors.toMap(Household::id, Function.identity()));

        // Collect all member user IDs from households to enrich with user details
        List<UUID> memberUserIds = householdsById.values().stream()
                .flatMap(h -> h.members().stream().map(HouseholdMember::userId))
                .distinct()
                .toList();

        Map<UUID, User> memberUsersById = memberUserIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(memberUserIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return invitations.stream()
                .map(invitation -> toSummary(invitation, usersById, householdsById, memberUsersById, userId))
                .toList();
    }

    private Household findHouseholdOrThrow(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(HouseholdNotFoundException::new);
    }

    private Invitation findInvitationOrThrow(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private boolean isUserParticipant(Household household, UUID userId) {
        return Optional.ofNullable(household)
                .map(Household::members)
                .map(members -> members.stream().anyMatch(member -> member.userId().equals(userId)))
                .orElse(false);
    }

    private void validateInvitationActive(Invitation invitation) {
        if (invitation.isNotPending()) {
            throw new InvitationAlreadyHandledException();
        }

        if (invitation.isExpired()) {
            throw new InvitationExpiredException();
        }
    }

    private InvitationSummary toSummary(Invitation invitation, Map<UUID, User> usersById, Map<UUID, Household> householdsById, Map<UUID, User> memberUsersById, UUID currentUserId) {
        User inviter = usersById.get(invitation.inviterUserId());
        User invitee = usersById.get(invitation.inviteeUserId());
        Household household = householdsById.get(invitation.householdId());

        // Enrich household members with user details
        List<HouseholdMemberSummary> enrichedMembers = household.members().stream()
                .map(member -> {
                    User memberUser = memberUsersById.get(member.userId());
                    return HouseholdMemberSummary.builder()
                            .id(member.userId())
                            .user(memberUser)
                            .householdLeader(false)
                            .status(member.status())
                            .joinedAt(member.joinedAt())
                            .build();
                })
                .toList();

        HouseholdSummary householdSummary = HouseholdSummary.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .members(enrichedMembers)
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();

        return InvitationSummary.builder()
                .id(invitation.id())
                .inviter(inviter)
                .invitee(invitee)
                .status(invitation.status())
                .household(householdSummary)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .isInviter(invitation.inviterUserId().equals(currentUserId))
                .build();
    }

    private InvitationSummary toSummary(Invitation invitation, UUID currentUserId) {
        List<UUID> userIds = List.of(invitation.inviterUserId(), invitation.inviteeUserId());
        Map<UUID, User> usersById = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));
        Household household = householdRepository.findById(invitation.householdId()).orElse(null);
        Map<UUID, Household> householdsById = household != null
                ? Map.of(household.id(), household)
                : Map.of();

        // Collect member user IDs to enrich with user details
        List<UUID> memberUserIds = household != null
                ? household.members().stream().map(HouseholdMember::userId).distinct().toList()
                : List.of();

        Map<UUID, User> memberUsersById = memberUserIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(memberUserIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return toSummary(invitation, usersById, householdsById, memberUsersById, currentUserId);
    }
}
