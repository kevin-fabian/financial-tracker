package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.exceptions.shared_space.SharedSpaceNotExistException;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SharingPermissionResolver;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSharedSpaceServiceTest {

    @Mock
    private SharedSpaceRepository spaceRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private SharingPermissionResolver permissionResolver;

    @InjectMocks
    private DefaultSharedSpaceService service;

    @Nested
    class SendInvitation {

        @Test
        void givenNullSpaceId_thenThrowsSharedSpaceNotExistException() {
            UUID inviterUserId = UUID.randomUUID();
            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "friend@example.com",
                    UUID.randomUUID(),
                    null,
                    "Trip Budget",
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE
            );

            assertThrows(SharedSpaceNotExistException.class, () -> service.sendInvitation(command));
            verify(spaceRepository, never()).save(any(SharedSpace.class));
            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void givenExistingSpaceWhereUserIsOwner_thenSendsInvitationToSpace() {
            UUID inviterUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace existingSpace = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(inviterUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(inviterUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.MUTUAL_SHARING)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "sibling@example.com",
                    UUID.randomUUID(),
                    spaceId,
                    null,
                    null,
                    AccessLevel.VIEW_ONLY
            );

            when(spaceRepository.findById(spaceId)).thenReturn(java.util.Optional.of(existingSpace));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Invitation result = service.sendInvitation(command);

            assertEquals(spaceId, result.sharedSpaceId());
            assertEquals(SharingMode.MUTUAL_SHARING, result.proposedSharingMode());
            verify(spaceRepository, never()).save(any(SharedSpace.class));
            verify(invitationRepository).save(any(Invitation.class));
        }

        @Test
        void givenExistingSpaceWhereUserIsNotOwner_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace existingSpace = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>())
                    .sharingMode(SharingMode.MUTUAL_SHARING)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "stranger@example.com",
                    UUID.randomUUID(),
                    spaceId,
                    null,
                    null,
                    AccessLevel.VIEW_ONLY
            );

            when(spaceRepository.findById(spaceId)).thenReturn(java.util.Optional.of(existingSpace));

            assertThrows(NotSpaceOwnerException.class, () -> service.sendInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenNullSpaceIdWithNullName_thenThrowsSharedSpaceNotExistException() {
            UUID inviterUserId = UUID.randomUUID();
            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "friend@example.com",
                    UUID.randomUUID(),
                    null,
                    null,
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE
            );

            assertThrows(SharedSpaceNotExistException.class, () -> service.sendInvitation(command));
            verify(spaceRepository, never()).save(any(SharedSpace.class));
            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void givenNullInviterUserId_thenThrows() {
            SendInvitationCommand command = new SendInvitationCommand(
                    null,
                    "someone@example.com",
                    UUID.randomUUID(),
                    null,
                    "New Space",
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE
            );

            assertThrows(IllegalArgumentException.class, () -> service.sendInvitation(command));
            verify(spaceRepository, never()).save(any());
            verify(invitationRepository, never()).save(any());
        }
    }

    @Nested
    class CreateShare {

        @Test
        void givenValidCommand_thenCreatesSpaceWithOwnerAsParticipant() {
            UUID ownerUserId = UUID.randomUUID();
            CreateSharedSpaceCommand command = new CreateSharedSpaceCommand(
                    ownerUserId,
                    "Trip Budget",
                    SharingMode.MUTUAL_SHARING,
                    List.of()
            );

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            SharedSpace result = service.createShare(command);

            assertNotNull(result);
            assertEquals("Trip Budget", result.spaceName());
            assertEquals(ownerUserId, result.ownerUserId());
            assertEquals(SharingMode.MUTUAL_SHARING, result.sharingMode());
            assertTrue(result.active());
            assertEquals(1, result.participants().size());

            SpaceParticipant owner = result.participants().getFirst();
            assertEquals(ownerUserId, owner.userId());
            assertEquals(AccessLevel.READ_WRITE, owner.accessLevel());
            assertEquals(ParticipantStatus.ACTIVE, owner.status());

            verify(spaceRepository).save(any(SharedSpace.class));
        }

        @Test
        void givenNullSpaceName_thenUsesDefaultName() {
            UUID ownerUserId = UUID.randomUUID();
            CreateSharedSpaceCommand command = new CreateSharedSpaceCommand(
                    ownerUserId,
                    null,
                    SharingMode.MUTUAL_SHARING,
                    List.of()
            );

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.createShare(command);

            assertEquals("Shared Space", captor.getValue().spaceName());
            verify(spaceRepository).save(any(SharedSpace.class));
        }

        @Test
        void givenNullOwnerUserId_thenThrows() {
            CreateSharedSpaceCommand command = new CreateSharedSpaceCommand(
                    null,
                    "My Space",
                    SharingMode.MUTUAL_SHARING,
                    null
            );

            assertThrows(IllegalArgumentException.class, () -> service.createShare(command));
            verify(spaceRepository, never()).save(any());
        }
    }

    @Nested
    class GetParticipantUserIds {

        @Test
        void givenUserBelongsToSpacesWithParticipants_thenReturnsDistinctParticipantUserIds() {
            UUID userId = UUID.randomUUID();
            UUID participant1 = UUID.randomUUID();
            UUID participant2 = UUID.randomUUID();
            List<UUID> expected = List.of(userId, participant1, participant2);

            when(spaceRepository.findParticipantUserIdsByUserId(userId)).thenReturn(expected);

            List<UUID> result = service.getParticipantUserIds(userId);

            assertEquals(3, result.size());
            assertTrue(result.containsAll(expected));
            verify(spaceRepository).findParticipantUserIdsByUserId(userId);
        }

        @Test
        void givenUserHasNoSpaces_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(spaceRepository.findParticipantUserIdsByUserId(userId)).thenReturn(List.of());

            List<UUID> result = service.getParticipantUserIds(userId);

            assertTrue(result.isEmpty());
            verify(spaceRepository).findParticipantUserIdsByUserId(userId);
        }

        @Test
        void givenUserSharesSpaceWithDuplicateParticipants_thenReturnsDistinctIds() {
            UUID userId = UUID.randomUUID();
            UUID otherParticipant = UUID.randomUUID();

            when(spaceRepository.findParticipantUserIdsByUserId(userId)).thenReturn(List.of(userId, otherParticipant));

            List<UUID> result = service.getParticipantUserIds(userId);

            assertEquals(2, result.size());
            assertEquals(Set.of(userId, otherParticipant), Set.copyOf(result));
            verify(spaceRepository).findParticipantUserIdsByUserId(userId);
        }
    }
}
