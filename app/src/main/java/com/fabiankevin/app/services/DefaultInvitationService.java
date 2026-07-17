package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.shared_space.*;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.PartyMemberStatus;
import com.fabiankevin.app.models.party.Invitation;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMember;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.services.commands.shared_space.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.invitations.SendInvitationCommand;
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
    private final PartyRepository spaceRepository;
    private final UserClient userClient;

    @Transactional
    @Override
    public Invitation sendInvitation(SendInvitationCommand command) {
        Party space = findSpaceOrThrow(command.partyId());
        if (!space.partyLeaderId().equals(command.inviterPlayerId())) {
            throw new NotSpaceOwnerException();
        }

        User invitee = userClient.getUserByEmail(command.inviteeEmail());

        if (isUserParticipant(space, invitee.id())) {
            throw new ParticipantAlreadyExistsException();
        }

        return invitationRepository.findPendingBySpaceIdAndInviterAndInvitee(command.partyId(), command.inviterPlayerId(), invitee.id())
                .orElseGet(() -> {
                    Invitation invitation = Invitation.builder()
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

                    return invitationRepository.save(invitation);
                });
    }

    @Transactional
    @Override
    public Party acceptInvitation(AcceptInvitationCommand command) {
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

        return spaceRepository.save(updatedSpace);
    }

    @Transactional
    @Override
    public Invitation rejectInvitation(RejectInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());

        if (!invitation.inviteePlayerId().equals(command.inviteeUserId())) {
            throw new ForbiddenException("Only the invited user can reject");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyHandledException();
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterPlayerId(invitation.inviterPlayerId())
                .inviteePlayerId(invitation.inviteePlayerId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.REJECTED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .sharedSpaceId(invitation.sharedSpaceId())
                .build();

        return invitationRepository.save(updatedInvitation);
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
                : spaceRepository.findAllById(spaceIds).stream()
                        .collect(Collectors.toMap(Party::id, Function.identity()));

        return invitations.stream()
                .map(invitation -> toSummary(invitation, usersById, spacesById))
                .toList();
    }

    private Party findSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(SharedSpaceNotFoundException::new);
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

    private InvitationSummary toSummary(Invitation invitation, Map<UUID, User> usersById, Map<UUID, Party> spacesById) {
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
                .sharedSpaceId(invitation.sharedSpaceId())
                .sharedSpaceName(space != null ? space.name() : null)
                .build();
    }

    private String deriveInitial(User user) {
        if (user == null || user.firstName() == null || user.lastName() == null) {
            return null;
        }
        return "" + user.firstName().charAt(0) + user.lastName().charAt(0);
    }
}
