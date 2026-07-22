package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.*;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.party.Invitation;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMember;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultInvitationService implements InvitationService {
    private final InvitationRepository invitationRepository;
    private final PartyRepository partyRepository;
    private final UserClient userClient;

    @Transactional
    @Override
    public InvitationSummary sendInvitation(SendInvitationCommand command) {
        Party space = findSpaceOrThrow(command.partyId());
        if (!space.partyLeaderId().equals(command.inviterPlayerId())) {
            throw new NotPartyLeaderException();
        }

        User invitee = userClient.getUserByEmail(command.inviteeEmail());

        if (isUserParticipant(space, invitee.id())) {
            throw new PartyMemberAlreadyExistsException();
        }

        if (partyRepository.findByPlayerId(invitee.id()).isPresent()) {
            throw new PartyMemberAlreadyExistsException();
        }

        Invitation invitation = invitationRepository.findPendingByPartyIdAndInviterAndInvitee(command.partyId(), command.inviterPlayerId(), invitee.id())
                .orElseGet(() -> {
                    Invitation newInvitation = Invitation.builder()
                            .inviterPlayerId(command.inviterPlayerId())
                            .inviteePlayerId(invitee.id())
                            .proposedSharingMode(space.sharingMode())
                            .proposedRole(AccessLevel.READ_WRITE)
                            .status(InvitationStatus.PENDING)
                            .createdAt(Instant.now())
                            .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                            .sharedSpaceId(space.id())
                            .build();

                    // TODO notify the recipient

                    return invitationRepository.save(newInvitation);
                });

        return toSummary(invitation, command.inviterPlayerId());
    }

    @Transactional
    @Override
    public InvitationSummary acceptInvitation(AcceptInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());
        validateInvitationActive(invitation);

        if (invitation.inviterPlayerId().equals(command.acceptingPlayerId())) {
            throw new InviterCannotAcceptOwnInvitationException();
        }

        if (!invitation.inviteePlayerId().equals(command.acceptingPlayerId())) {
            throw new ForbiddenException("Only the invited user can accept");
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterPlayerId(invitation.inviterPlayerId())
                .inviteePlayerId(invitation.inviteePlayerId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.ACCEPTED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .sharedSpaceId(invitation.sharedSpaceId())
                .build();
        invitationRepository.save(updatedInvitation);

        Party space = findSpaceOrThrow(invitation.sharedSpaceId());

        PartyMember participant = PartyMember.builder()
                .playerId(invitation.inviteePlayerId())
                .accessLevel(invitation.proposedRole())
                .status(PartyMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        List<PartyMember> updatedParticipants = new ArrayList<>(space.partyMembers());
        updatedParticipants.add(participant);

        Party updatedSpace = space.toBuilder()
                .partyMembers(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        partyRepository.save(updatedSpace);

        return toSummary(updatedInvitation, command.acceptingPlayerId());
    }

    @Transactional
    @Override
    public InvitationSummary rejectInvitation(RejectInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());

        boolean isInvitee = invitation.inviteePlayerId().equals(command.rejectingUserId());
        boolean isInviter = invitation.inviterPlayerId().equals(command.rejectingUserId());

        if (!isInvitee && !isInviter) {
            throw new ForbiddenException("Only the invited user or the inviter can reject the invitation");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyHandledException();
        }

        invitationRepository.delete(invitation.id());

        return toSummary(invitation, command.rejectingUserId());
    }

    @Override
    public List<InvitationSummary> getInvitationsByUserId(UUID userId) {
        List<Invitation> invitations = invitationRepository.findByInviterUserIdOrInviteeUserId(userId);

        List<UUID> userIds = invitations.stream()
                .flatMap(invitation -> List.of(invitation.inviterPlayerId(), invitation.inviteePlayerId()).stream())
                .distinct()
                .toList();

        Map<UUID, User> usersById = userIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(userIds).stream()
                        .collect(Collectors.toMap(User::id, Function.identity()));

        List<UUID> spaceIds = invitations.stream()
                .map(Invitation::sharedSpaceId)
                .distinct()
                .toList();

        Map<UUID, Party> spacesById = spaceIds.isEmpty()
                ? Map.of()
                : partyRepository.findAllById(spaceIds).stream()
                        .collect(Collectors.toMap(Party::id, Function.identity()));

        return invitations.stream()
                .map(invitation -> toSummary(invitation, usersById, spacesById, userId))
                .toList();
    }

    private Party findSpaceOrThrow(UUID spaceId) {
        return partyRepository.findById(spaceId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private Invitation findInvitationOrThrow(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private boolean isUserParticipant(Party space, UUID userId) {
        return space.partyMembers().stream()
                .anyMatch(spaceParticipant -> spaceParticipant.playerId().equals(userId));
    }

    private void validateInvitationActive(Invitation invitation) {
        if (invitation.isNotPending()) {
            throw new InvitationAlreadyHandledException();
        }

        if (invitation.isExpired()) {
            throw new InvitationExpiredException();
        }
    }

    private InvitationSummary toSummary(Invitation invitation, Map<UUID, User> usersById, Map<UUID, Party> spacesById, UUID currentUserId) {
        User inviter = usersById.get(invitation.inviterPlayerId());
        User invitee = usersById.get(invitation.inviteePlayerId());
        Party space = spacesById.get(invitation.sharedSpaceId());

        return InvitationSummary.builder()
                .id(invitation.id())
                .inviterName(inviter != null ? inviter.firstName() + " " + inviter.lastName() : null)
                .inviterInitial(deriveInitial(inviter))
                .inviteeName(invitee != null ? invitee.firstName() + " " + invitee.lastName() : null)
                .inviteeInitial(deriveInitial(invitee))
                .proposedSharingModeName(invitation.proposedSharingMode() != null ? invitation.proposedSharingMode().getName() : null)
                .proposedSharingModeDescription(invitation.proposedSharingMode() != null ? invitation.proposedSharingMode().getDescription() : null)
                .proposedRoleName(invitation.proposedRole() != null ? invitation.proposedRole().getName() : null)
                .proposedRoleDescription(invitation.proposedRole() != null ? invitation.proposedRole().getDescription() : null)
                .status(invitation.status())
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .partyId(invitation.sharedSpaceId())
                .partyName(space != null ? space.name() : null)
                .inviter(invitation.inviterPlayerId().equals(currentUserId))
                .build();
    }

    private InvitationSummary toSummary(Invitation invitation, UUID currentUserId) {
        List<UUID> userIds = List.of(invitation.inviterPlayerId(), invitation.inviteePlayerId());
        Map<UUID, User> usersById = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));
        Party space = partyRepository.findById(invitation.sharedSpaceId()).orElse(null);
        Map<UUID, Party> spacesById = space != null
                ? Map.of(space.id(), space)
                : Map.of();
        return toSummary(invitation, usersById, spacesById, currentUserId);
    }

    private String deriveInitial(User user) {
        if (user == null || user.firstName() == null || user.lastName() == null) {
            return null;
        }
        return "" + user.firstName().charAt(0) + user.lastName().charAt(0);
    }
}
