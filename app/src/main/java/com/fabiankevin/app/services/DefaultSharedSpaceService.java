package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.*;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.shared_space.*;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultSharedSpaceService implements SharedSpaceService {
    private final SharedSpaceRepository spaceRepository;
    private final InvitationRepository invitationRepository;
    private final SharingPermissionResolver permissionResolver;

    @Transactional
    @Override
    public SharedSpace createShare(CreateSharedSpaceCommand command) {
        validateCreateCommand(command);

        List<SpaceParticipant> initialParticipants = new ArrayList<>();
        initialParticipants.add(SpaceParticipant.builder()
                .userId(command.ownerUserId())
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .sharingRule(null)
                .build());

        List<SharedResource> sharedResources = new ArrayList<>();
        for (AddSharedResourceCommand resource : command.resources()) {
            sharedResources.add(SharedResource.builder()
                    .type(resource.type())
                    .items(resource.itemIds())
                    .sharedAt(Instant.now())
                    .build());
        }

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
    public Invitation sendInvitation(SendInvitationCommand command) {
        validateSendCommand(command);

        SharedSpace space;
        if (command.spaceId() != null) {
            space = findSpaceOrThrow(command.spaceId());
            if (!space.ownerUserId().equals(command.inviterUserId())) {
                throw new NotSpaceOwnerException();
            }
        } else {
            space = createNewSpace(command);
        }

        if (isUserParticipant(space, command.inviteeEmail())) {
            throw new ParticipantAlreadyExistsException();
        }

        Invitation invitation = Invitation.builder()
                .inviterUserId(command.inviterUserId())
                .inviteeEmail(command.inviteeEmail())
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
    }

    @Transactional
    @Override
    public SharedSpace acceptInvitation(AcceptInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());
        validateInvitationActive(invitation);

        invitation = Invitation.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeEmail(invitation.inviteeEmail())
                .inviteeUserId(command.acceptingUserId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.ACCEPTED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .sharedSpaceId(invitation.sharedSpaceId())
                .build();
        invitationRepository.save(invitation);

        SharedSpace space = findSpaceOrThrow(invitation.sharedSpaceId());

        SpaceParticipant participant = SpaceParticipant.builder()
                .userId(command.acceptingUserId())
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

        if (!invitation.inviteeEmail().equals(command.userEmail())) {
            throw new ForbiddenException("Only the invited user can reject");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only pending invitations can be rejected");
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeEmail(invitation.inviteeEmail())
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

    @Transactional
    @Override
    public Invitation revokeInvitation(RevokeInvitationCommand command) {
        Invitation invitation = findInvitationOrThrow(command.invitationId());

        if (!invitation.inviterUserId().equals(command.revokerUserId())) {
            throw new ForbiddenException("Only the inviter can revoke");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only pending invitations can be revoked");
        }

        Invitation updatedInvitation = Invitation.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeEmail(invitation.inviteeEmail())
                .inviteeUserId(invitation.inviteeUserId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(InvitationStatus.REVOKED)
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .sharedSpaceId(invitation.sharedSpaceId())
                .build();

        return invitationRepository.save(updatedInvitation);
    }

    @Transactional
    @Override
    public SharedSpace updateParticipantRule(UUID spaceId, UUID participantId, SharingRule rule) {
        SharedSpace space = findSpaceOrThrow(spaceId);

        List<SpaceParticipant> updatedParticipants = space.participants().stream()
                .map(p -> p.userId().equals(participantId)
                        ? p.toBuilder().sharingRule(rule).build()
                        : p)
                .collect(Collectors.toList());

        SharedSpace updatedSpace = space.toBuilder()
                .participants(updatedParticipants)
                .updatedAt(Instant.now())
                .build();

        return spaceRepository.save(updatedSpace);
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
    public List<SharedSpace> retrieveByUserId(UUID userId) {
        return spaceRepository.retrieveByUserId(userId);
    }

    @Override
    public List<SharedResource> getVisibleResources(UUID spaceId, UUID viewerId) {
        SharedSpace space = findSpaceOrThrow(spaceId);
//        SpaceParticipant viewer = findParticipantOrThrow(space, viewerId);

//        SharingRule rule = permissionResolver.resolveRule(space, viewer);

        return space.sharedResources().stream()
                .filter(resource -> permissionResolver.canViewResource(space, viewerId, resource.type()))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public SharedResource addResource(UUID spaceId, AddSharedResourceCommand command) {
        SharedSpace space = findSpaceOrThrow(spaceId);
        SpaceParticipant contributor = findParticipantOrThrow(space, command.ownerUserId());

        SharingRule rule = permissionResolver.resolveRule(space, contributor);
        if (!rule.sharesOwnResources()) {
            throw new ForbiddenException("User is not allowed to share resources");
        }

        if (!rule.visibleResourceTypes().contains(command.type())) {
            throw new IllegalArgumentException("Resource type not allowed by sharing rules");
        }

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

    private SharedSpace findSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private Invitation findInvitationOrThrow(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private SpaceParticipant findParticipantOrThrow(SharedSpace space, UUID userId) {
        return space.participants().stream()
                .filter(p -> p.userId().equals(userId))
                .findFirst()
                .orElseThrow(SharedSpaceNotFoundException::new);
    }

    private void validateSendCommand(SendInvitationCommand command) {
        if (command.inviterUserId() == null) {
            throw new IllegalArgumentException("Inviter ID cannot be null");
        }
        if (command.inviteeEmail() == null || command.inviteeEmail().isBlank()) {
            throw new IllegalArgumentException("Invitee email cannot be null or blank");
        }
        if (command.proposedRole() == null) {
            throw new IllegalArgumentException("Proposed role cannot be null");
        }
        if (command.spaceId() == null && command.sharingMode() == null) {
            throw new IllegalArgumentException("Sharing mode required when creating new space");
        }
    }

    private void validateCreateCommand(CreateSharedSpaceCommand command) {
        if (command.ownerUserId() == null) {
            throw new IllegalArgumentException("Owner user ID cannot be null");
        }
        if (command.sharingMode() == null) {
            throw new IllegalArgumentException("Sharing mode cannot be null");
        }
    }

    private SharedSpace createNewSpace(SendInvitationCommand command) {
        List<SpaceParticipant> initialParticipants = new ArrayList<>();

        initialParticipants.add(SpaceParticipant.builder()
                .userId(command.inviterUserId())
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .sharingRule(null)
                .build());

        SharedSpace newSpace = SharedSpace.builder()
                .spaceName(command.spaceName() != null ? command.spaceName() : "Shared Space")
                .ownerUserId(command.inviterUserId())
                .participants(initialParticipants)
                .sharingMode(command.sharingMode())
                .sharedResources(new ArrayList<>())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return spaceRepository.save(newSpace);
    }

    private boolean isUserParticipant(SharedSpace space, String email) {
        // Placeholder: in a real implementation, look up user by email first,
        // then check if that user ID is a participant.
        return false;
    }

    private void validateInvitationActive(Invitation invitation) {
        if (invitation.isNotPending()) {
            throw new IllegalStateException("Invitation is not pending");
        }

        if (invitation.isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }
    }
}
