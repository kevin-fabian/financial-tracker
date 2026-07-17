package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.*;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.Invitation;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMember;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultInvitationServiceTest {

    @Mock
    private PartyRepository spaceRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultInvitationService service;

    @Nested
    class SendInvitation {

        @Test
        void givenNullSpaceId_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> new SendInvitationCommand(
                    inviterUserId,
                    "jane@example.com",
                    null
            ));
        }

        @Test
        void givenExistingSpaceWhereUserIsOwner_thenSendsInvitationToSpace() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Party existingSpace = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(inviterUserId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(inviterUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    spaceId
            );

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(existingSpace));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());
            when(invitationRepository.findPendingBySpaceIdAndInviterAndInvitee(spaceId, inviterUserId, inviteeUserId))
                    .thenReturn(Optional.empty());
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Invitation result = service.sendInvitation(command);

            assertEquals(spaceId, result.sharedSpaceId());
            assertEquals(inviteeUserId, result.inviteePlayerId());
            assertEquals(SharingMode.EVEN_SHARE, result.proposedSharingMode());
            assertEquals(InvitationStatus.PENDING, result.status());
            verify(spaceRepository, never()).save(any(Party.class));
            verify(invitationRepository).save(any(Invitation.class));
        }

        @Test
        void givenExistingPendingInvitation_thenReturnsExistingInvitation() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Party existingSpace = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(inviterUserId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(inviterUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Invitation existingInvitation = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    spaceId
            );

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(existingSpace));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());
            when(invitationRepository.findPendingBySpaceIdAndInviterAndInvitee(spaceId, inviterUserId, inviteeUserId))
                    .thenReturn(Optional.of(existingInvitation));

            Invitation result = service.sendInvitation(command);

            assertEquals(existingInvitation.id(), result.id());
            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void givenExistingSpaceWhereUserIsNotOwner_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Party existingSpace = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .partyMembers(new ArrayList<>())
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    spaceId
            );

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(existingSpace));

            assertThrows(NotPartyLeaderException.class, () -> service.sendInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenInviteeIsAlreadyParticipant_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Party existingSpace = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(inviterUserId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(inviterUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(inviteeUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    spaceId);

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(existingSpace));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());

            assertThrows(PartyMemberAlreadyExistsException.class, () -> service.sendInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenNullInviterUserId_thenThrows() {
            assertThrows(NullPointerException.class, () -> new SendInvitationCommand(
                    null,
                    "jane@example.com",
                    UUID.randomUUID()
            ));
        }

        @Test
        void givenNullInviteeEmail_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> new SendInvitationCommand(
                    inviterUserId,
                    null,
                    UUID.randomUUID()
            ));
        }
    }

    @Nested
    class AcceptInvitation {

        @Test
        void givenValidPendingInvitation_thenAcceptsAndAddsInviteeAsParticipant() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(inviterUserId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(inviterUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, inviteeUserId);

            Party result = service.acceptInvitation(command);

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(invitationCaptor.capture());
            assertEquals(InvitationStatus.ACCEPTED, invitationCaptor.getValue().status());
            assertEquals(inviteeUserId, invitationCaptor.getValue().inviteePlayerId());
            assertNotNull(result);
            assertEquals(2, result.partyMembers().size());
            PartyMember addedParticipant = result.partyMembers().stream()
                    .filter(p -> p.playerId().equals(inviteeUserId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Invitee should be added as participant"));
            assertEquals(AccessLevel.VIEW_ONLY, addedParticipant.accessLevel());
            assertEquals(PartyMemberStatus.ACTIVE, addedParticipant.status());
            assertNotNull(addedParticipant.joinedAt());
            verify(spaceRepository).save(any(Party.class));
        }

        @Test
        void givenInvitationDoesNotExist_thenThrows() {
            UUID invitationId = UUID.randomUUID();
            UUID acceptingUserId = UUID.randomUUID();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, acceptingUserId);

            assertThrows(InvitationNotFoundException.class, () -> service.acceptInvitation(command));
            verify(invitationRepository, never()).save(any());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenInvitationAlreadyHandled_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, inviteeUserId);

            assertThrows(InvitationAlreadyHandledException.class, () -> service.acceptInvitation(command));
            verify(invitationRepository, never()).save(any());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenExpiredInvitation_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now().minusSeconds(172800))
                    .expiresAt(Instant.now().minusSeconds(60))
                    .sharedSpaceId(spaceId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, inviteeUserId);

            assertThrows(InvitationExpiredException.class, () -> service.acceptInvitation(command));
            verify(invitationRepository, never()).save(any());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenInviterAttemptsToAcceptOwnInvitation_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(UUID.randomUUID())
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, inviterUserId);

            assertThrows(InviterCannotAcceptOwnInvitationException.class, () -> service.acceptInvitation(command));
            verify(invitationRepository, never()).save(any());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenNonInviteeAttemptsToAccept_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, otherUserId);

            assertThrows(ForbiddenException.class, () -> service.acceptInvitation(command));
            verify(invitationRepository, never()).save(any());
            verify(spaceRepository, never()).save(any());
        }
    }

    @Nested
    class GetInvitationsByUserId {

        @Test
        void givenUserIsInviterOrInvitee_thenReturnsMatchingInvitations() {
            UUID userId = UUID.randomUUID();
            UUID inviterId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            Invitation sent = Invitation.builder()
                .id(UUID.randomUUID())
                .inviterPlayerId(userId)
                .inviteePlayerId(inviteeUserId)
                .proposedSharingMode(SharingMode.EVEN_SHARE)
                .proposedRole(AccessLevel.READ_WRITE)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(604800))
                .sharedSpaceId(spaceId)
                .build();
            Invitation received = Invitation.builder()
                .id(UUID.randomUUID())
                .inviterPlayerId(inviterId)
                .inviteePlayerId(userId)
                .proposedSharingMode(SharingMode.EVEN_SHARE)
                .proposedRole(AccessLevel.VIEW_ONLY)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(604800))
                .sharedSpaceId(spaceId)
                .build();

            when(invitationRepository.findByInviterUserIdOrInviteeUserId(userId))
                .thenReturn(List.of(sent, received));
            when(userClient.getUsersByIds(List.of(userId, inviteeUserId, inviterId)))
                .thenReturn(List.of(
                    User.builder().id(userId).firstName("John").lastName("Doe").build(),
                    User.builder().id(inviteeUserId).firstName("Jane").lastName("Smith").build(),
                    User.builder().id(inviterId).firstName("Bob").lastName("Jones").build()));
            when(spaceRepository.findAllById(List.of(spaceId)))
                .thenReturn(List.of(Party.builder()
                    .id(spaceId)
                    .name("Family 2026 Budget")
                    .partyLeaderId(inviterId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedItems(List.of())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()));

            List<InvitationSummary> result = service.getInvitationsByUserId(userId);

            assertEquals(2, result.size());
            verify(invitationRepository).findByInviterUserIdOrInviteeUserId(userId);
            verify(userClient).getUsersByIds(List.of(userId, inviteeUserId, inviterId));
            verify(spaceRepository).findAllById(List.of(spaceId));
        }

        @Test
        void givenUserHasNoInvitations_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(invitationRepository.findByInviterUserIdOrInviteeUserId(userId)).thenReturn(List.of());

            List<InvitationSummary> result = service.getInvitationsByUserId(userId);

            assertTrue(result.isEmpty());
            verify(invitationRepository).findByInviterUserIdOrInviteeUserId(userId);
            verifyNoInteractions(userClient);
        }
    }

    @Nested
    class RejectInvitation {

        @Test
        void givenInviteeRejectsPendingInvitation_thenReturnsRejectedInvitation() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(inviterUserId)
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(spaceId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, inviteeUserId);

            Invitation result = service.rejectInvitation(command);

            ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(captor.capture());
            assertEquals(InvitationStatus.REJECTED, captor.getValue().status());
            assertEquals(InvitationStatus.REJECTED, result.status());
            assertEquals(inviteeUserId, result.inviteePlayerId());
            assertEquals(inviterUserId, result.inviterPlayerId());
            assertEquals(spaceId, result.sharedSpaceId());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenNonInviteeAttemptsToReject_thenThrows() {
            UUID inviteeUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(UUID.randomUUID())
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(UUID.randomUUID())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, otherUserId);

            assertThrows(ForbiddenException.class, () -> service.rejectInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenInvitationAlreadyHandled_thenThrows() {
            UUID inviteeUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterPlayerId(UUID.randomUUID())
                    .inviteePlayerId(inviteeUserId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .sharedSpaceId(UUID.randomUUID())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, inviteeUserId);

            assertThrows(InvitationAlreadyHandledException.class, () -> service.rejectInvitation(command));
            verify(invitationRepository, never()).save(any());
        }
    }
}
