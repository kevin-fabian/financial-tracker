package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.HouseholdMemberAlreadyExistsException;
import com.fabiankevin.app.exceptions.party.InvitationAlreadyHandledException;
import com.fabiankevin.app.exceptions.party.InvitationExpiredException;
import com.fabiankevin.app.exceptions.party.InvitationNotFoundException;
import com.fabiankevin.app.exceptions.party.InviterCannotAcceptOwnInvitationException;
import com.fabiankevin.app.exceptions.party.NotHouseholdLeaderException;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdMember;
import com.fabiankevin.app.models.household.Invitation;
import com.fabiankevin.app.models.household.InvitationSummary;
import com.fabiankevin.app.persistence.HouseholdRepository;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.services.commands.household.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.SendInvitationCommand;
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

import static com.fabiankevin.app.models.enums.household.AccessLevel.VIEW_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInvitationServiceTest {

    @Mock
    private HouseholdRepository spaceRepository;

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
        void givenExistingPartyWhereUserIsPartyLeader_thenSendsInvitationToParty() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Household existingHousehold = Household.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .leaderId(inviterUserId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(inviterUserId)
                                    .accessLevel(VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    partyId
            );

            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(existingHousehold));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());
            when(userClient.getUsersByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(
                            User.builder().id(inviterUserId).firstName("John").lastName("Doe").build(),
                            User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build()
                    ));
            when(invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(partyId, inviterUserId, inviteeUserId))
                    .thenReturn(Optional.empty());
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            InvitationSummary result = service.sendInvitation(command);

            assertEquals(partyId, result.household().id());
            assertTrue(result.isInviter());
            assertEquals(InvitationStatus.PENDING, result.status());
            assertEquals("John Doe", result.inviterName());
            assertEquals("Jane Doe", result.inviteeName());
            verify(spaceRepository, never()).save(any(Household.class));
            verify(invitationRepository).save(any(Invitation.class));
        }

        @Test
        void givenExistingPendingInvitation_thenReturnsExistingInvitation() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Household existingSpace = Household.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .leaderId(inviterUserId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(inviterUserId)
                                    .accessLevel(VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Invitation existingInvitation = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    partyId
            );

            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(existingSpace));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());
            when(userClient.getUsersByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(
                            User.builder().id(inviterUserId).firstName("John").lastName("Doe").build(),
                            User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build()
                    ));
            when(invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(partyId, inviterUserId, inviteeUserId))
                    .thenReturn(Optional.of(existingInvitation));

            InvitationSummary result = service.sendInvitation(command);

            assertEquals(existingInvitation.id(), result.id());
            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void givenExistingPartyWhereUserIsNotPartyLeader_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            UUID partyLeaderId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Household existingSpace = Household.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    partyId
            );

            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(existingSpace));

            assertThrows(NotHouseholdLeaderException.class, () -> service.sendInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenInviteeIsAlreadyPartyMember_thenThrows() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            Household existingSpace = Household.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .leaderId(inviterUserId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(inviterUserId)
                                    .accessLevel(VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .userId(inviteeUserId)
                                    .accessLevel(VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SendInvitationCommand command = new SendInvitationCommand(
                    inviterUserId,
                    inviteeEmail,
                    partyId);

            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(existingSpace));
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeUserId).firstName("Jane").lastName("Doe").build());

            assertThrows(HouseholdMemberAlreadyExistsException.class, () -> service.sendInvitation(command));
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
        void givenValidPendingInvitation_thenAcceptsAndAddsInviteeAsPartyMember() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
                    .build();
            Household space = Household.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .leaderId(inviterUserId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(inviterUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(
                            User.builder().id(inviterUserId).firstName("John").lastName("Doe").build(),
                            User.builder().id(inviteeUserId).firstName("Jane").lastName("Smith").build()
                    ));

            AcceptInvitationCommand command = new AcceptInvitationCommand(invitationId, inviteeUserId);

            InvitationSummary result = service.acceptInvitation(command);

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(invitationCaptor.capture());
            assertEquals(InvitationStatus.ACCEPTED, invitationCaptor.getValue().status());
            assertEquals(inviteeUserId, invitationCaptor.getValue().inviteeUserId());
            assertNotNull(result);
            assertEquals(InvitationStatus.ACCEPTED, result.status());
            assertEquals(partyId, result.household().id());
            assertFalse(result.isInviter());
            ArgumentCaptor<Household> spaceCaptor = ArgumentCaptor.forClass(Household.class);
            verify(spaceRepository).save(spaceCaptor.capture());
            Household savedSpace = spaceCaptor.getValue();
            assertEquals(2, savedSpace.members().size());
            HouseholdMember addedParticipant = savedSpace.members().stream()
                    .filter(p -> p.userId().equals(inviteeUserId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Invitee should be added as participant"));
            assertEquals(AccessLevel.VIEW_ONLY, addedParticipant.accessLevel());
            assertEquals(HouseholdMemberStatus.ACTIVE, addedParticipant.status());
            assertNotNull(addedParticipant.joinedAt());
            verify(spaceRepository).save(any(Household.class));
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
            UUID partyId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
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
            UUID partyId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now().minusSeconds(172800))
                    .expiresAt(Instant.now().minusSeconds(60))
                    .householdId(partyId)
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
            UUID partyId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(UUID.randomUUID())
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
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
            UUID partyId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
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
            UUID partyId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            Invitation sent = Invitation.builder()
                .id(UUID.randomUUID())
                .inviterUserId(userId)
                .inviteeUserId(inviteeUserId)
                .proposedRole(AccessLevel.VIEW_ONLY)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(604800))
                .householdId(partyId)
                .build();
            Invitation received = Invitation.builder()
                .id(UUID.randomUUID())
                .inviterUserId(inviterId)
                .inviteeUserId(userId)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(604800))
                .householdId(partyId)
                .build();

            when(invitationRepository.findByInviterUserIdOrInviteeUserId(userId))
                .thenReturn(List.of(sent, received));
            when(userClient.getUsersByIds(List.of(userId, inviteeUserId, inviterId)))
                .thenReturn(List.of(
                    User.builder().id(userId).firstName("John").lastName("Doe").build(),
                    User.builder().id(inviteeUserId).firstName("Jane").lastName("Smith").build(),
                    User.builder().id(inviterId).firstName("Bob").lastName("Jones").build()));
            when(spaceRepository.findAllById(List.of(partyId)))
                .thenReturn(List.of(Household.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .leaderId(inviterId)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()));

            List<InvitationSummary> result = service.getInvitationsByUserId(userId);

            assertEquals(2, result.size());
            InvitationSummary sentSummary = result.getFirst();
            assertEquals(partyId, sentSummary.household().id());
            assertTrue(sentSummary.isInviter());
            InvitationSummary receivedSummary = result.get(1);
            assertEquals(partyId, receivedSummary.household().id());
            assertFalse(receivedSummary.isInviter());
            verify(invitationRepository).findByInviterUserIdOrInviteeUserId(userId);
            verify(userClient).getUsersByIds(List.of(userId, inviteeUserId, inviterId));
            verify(spaceRepository).findAllById(List.of(partyId));
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
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(userClient.getUsersByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(
                            User.builder().id(inviterUserId).firstName("John").lastName("Doe").build(),
                            User.builder().id(inviteeUserId).firstName("Jane").lastName("Smith").build()
                    ));
            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(
                    Household.builder().id(partyId).name("Test Household").leaderId(inviterUserId).active(true).members(List.of()).createdAt(Instant.now()).build()
            ));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, inviteeUserId);

            InvitationSummary result = service.rejectInvitation(command);

            ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(captor.capture());
            verify(invitationRepository, never()).delete(any());
            assertEquals(InvitationStatus.CANCELLED, captor.getValue().status());
            assertEquals(partyId, result.household().id());
            assertFalse(result.isInviter());
            assertEquals("John Doe", result.inviterName());
            assertEquals("Jane Smith", result.inviteeName());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenNonInviteeOrNonInviterAttemptsToReject_thenThrows() {
            UUID inviteeUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(UUID.randomUUID())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            lenient().when(spaceRepository.findById(invitation.householdId())).thenReturn(Optional.empty());

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, otherUserId);

            assertThrows(ForbiddenException.class, () -> service.rejectInvitation(command));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void givenInviterCancelsInvitation_thenInvitationIsDeleted() {
            UUID inviterUserId = UUID.randomUUID();
            UUID inviteeUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(inviterUserId)
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(partyId)
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(userClient.getUsersByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(
                            User.builder().id(inviterUserId).firstName("John").lastName("Doe").build(),
                            User.builder().id(inviteeUserId).firstName("Jane").lastName("Smith").build()
                    ));
            when(spaceRepository.findById(partyId)).thenReturn(Optional.of(
                    Household.builder().id(partyId).name("Test Household").leaderId(inviterUserId).active(true).members(List.of()).createdAt(Instant.now()).build()
            ));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, inviterUserId);

            InvitationSummary result = service.rejectInvitation(command);

            ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(captor.capture());
            verify(invitationRepository, never()).delete(any());
            assertEquals(InvitationStatus.CANCELLED, captor.getValue().status());
            assertEquals(partyId, result.household().id());
            assertTrue(result.isInviter());
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenInvitationAlreadyHandled_thenThrows() {
            UUID inviteeUserId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            Invitation invitation = Invitation.builder()
                    .id(invitationId)
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(inviteeUserId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .householdId(UUID.randomUUID())
                    .build();

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            RejectInvitationCommand command = new RejectInvitationCommand(invitationId, inviteeUserId);

            assertThrows(InvitationAlreadyHandledException.class, () -> service.rejectInvitation(command));
            verify(invitationRepository, never()).save(any());
        }
    }
}
