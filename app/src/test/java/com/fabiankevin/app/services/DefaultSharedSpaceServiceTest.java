package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.models.enums.AccessLevel;
import com.fabiankevin.app.models.enums.InvitationStatus;
import com.fabiankevin.app.models.enums.ParticipantStatus;
import com.fabiankevin.app.models.enums.SharingMode;
import com.fabiankevin.app.models.shared_space.*;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.SendInvitationCommand;
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
        void givenNewSpaceRequest_thenCreatesSpaceAndSendsInvitation() {
            UUID inviterUserId = UUID.randomUUID();
            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "friend@example.com",
                    null,
                    "Trip Budget",
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE,
                    SharingRule.MUTUAL_DEFAULT,
                    SharingRule.MUTUAL_DEFAULT
            );

            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Invitation result = service.sendInvitation(command);

            assertNotNull(result);
            assertEquals(InvitationStatus.PENDING, result.status());
            assertEquals(inviterUserId, result.inviterUserId());
            assertEquals("friend@example.com", result.inviteeEmail());
            assertEquals(AccessLevel.READ_WRITE, result.proposedRole());

            verify(spaceRepository).save(any(SharedSpace.class));
            verify(invitationRepository).save(any(Invitation.class));
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
                    spaceId,
                    null,
                    null,
                    AccessLevel.VIEW_ONLY,
                    SharingRule.VIEWER_DEFAULT,
                    null
            );

            when(spaceRepository.findById(spaceId)).thenReturn(java.util.Optional.of(existingSpace));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Invitation result = service.sendInvitation(command);

            assertEquals(spaceId, result.resultingSharedSpaceId());
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
                    spaceId,
                    null,
                    null,
                    AccessLevel.VIEW_ONLY,
                    SharingRule.VIEWER_DEFAULT,
                    null
            );

            when(spaceRepository.findById(spaceId)).thenReturn(java.util.Optional.of(existingSpace));

            assertThrows(NotSpaceOwnerException.class, () -> service.sendInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenNewSpaceWithNullName_thenUsesDefaultName() {
            UUID inviterUserId = UUID.randomUUID();
            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    "friend@example.com",
                    null,
                    null,
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE,
                    SharingRule.MUTUAL_DEFAULT,
                    SharingRule.MUTUAL_DEFAULT
            );

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.sendInvitation(command);

            assertEquals("Shared Space", captor.getValue().spaceName());
        }

        @Test
        void givenNullInviterUserId_thenThrows() {
            SendInvitationCommand command = new SendInvitationCommand(
                    null,
                    "someone@example.com",
                    null,
                    "New Space",
                    SharingMode.MUTUAL_SHARING,
                    AccessLevel.READ_WRITE,
                    SharingRule.MUTUAL_DEFAULT,
                    SharingRule.MUTUAL_DEFAULT
            );

            assertThrows(IllegalArgumentException.class, () -> service.sendInvitation(command));
            verify(spaceRepository, never()).save(any());
            verify(invitationRepository, never()).save(any());
        }
    }
}
