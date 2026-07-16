package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.*;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultInvitationService implements InvitationService {
    private final InvitationRepository invitationRepository;
    private final SharedSpaceRepository spaceRepository;

    @Transactional
    @Override
    public Invitation sendInvitation(SendInvitationCommand command) {
        SharedSpace space = findSpaceOrThrow(command.spaceId());
        if (!space.ownerUserId().equals(command.inviterUserId())) {
            throw new NotSpaceOwnerException();
        }

        if (isUserParticipant(space, command.inviteeUserId())) {
            throw new ParticipantAlreadyExistsException();
        }

        return invitationRepository.findPendingByInviterAndInvitee(command.inviterUserId(), command.inviteeUserId())
                .orElseGet(() -> {
                    Invitation invitation = Invitation.builder()
                            .inviterUserId(command.inviterUserId())
                            .inviteeUserId(command.inviteeUserId())
                            .proposedSharingMode(space.sharingMode())
                            .proposedRole(command.proposedRole())
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
    public SharedSpace acceptInvitation(AcceptInvitationCommand command) {
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
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.ACCEPTED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .sharedSpaceId(invitation.sharedSpaceId())
                .build();
        invitationRepository.save(updatedInvitation);

        SharedSpace space = findSpaceOrThrow(invitation.sharedSpaceId());

        SpaceParticipant participant = SpaceParticipant.builder()
                .userId(invitation.inviteeUserId())
                .accessLevel(invitation.proposedRole())
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        List<SpaceParticipant> updatedParticipants = new ArrayList<>(space.participants());
        updatedParticipants.add(participant);

        SharedSpace updatedSpace = space.toBuilder()
                .participants(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        return spaceRepository.save(updatedSpace);
    }

    @Transactional
    @Override
    public Invitation rejectInvitation(RejectInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());

        if (!invitation.inviteeUserId().equals(command.inviteeUserId())) {
            throw new ForbiddenException("Only the invited user can reject");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyHandledException();
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeUserId(invitation.inviteeUserId())
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
    public List<Invitation> getInvitationsByUserId(UUID userId) {
        return invitationRepository.findByInviterUserIdOrInviteeUserId(userId);
    }

    private SharedSpace findSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private Invitation findInvitationOrThrow(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private boolean isUserParticipant(SharedSpace space, UUID userId) {
        return space.participants().stream()
                .anyMatch(spaceParticipant -> spaceParticipant.userId().equals(userId));
    }

    private void validateInvitationActive(Invitation invitation) {
        if (invitation.isNotPending()) {
            throw new InvitationAlreadyHandledException();
        }

        if (invitation.isExpired()) {
            throw new InvitationExpiredException();
        }
    }
}
