package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.HouseholdAlreadyExistsException;
import com.fabiankevin.app.exceptions.party.HouseholdNotFoundException;
import com.fabiankevin.app.exceptions.party.NotHouseholdLeaderException;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.User;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultHouseholdServiceTest {
    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultHouseholdService service;

    @Nested
    class OrganizeHousehold {
        @Test
        void givenValidCommand_thenCreatesHouseholdWithOwnerAsHouseholdMember() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizeHouseholdCommand command = new OrganizeHouseholdCommand(
                    partyLeaderId,
                    "Trip Budget"
            );

            ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
            when(householdRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            HouseholdSummary result = service.organize(command);

            assertNotNull(result);
            assertEquals("Trip Budget", result.name());
            assertEquals(partyLeaderId, result.leaderId());
            assertTrue(result.active());
            assertEquals(1, result.members().size());

            HouseholdMemberSummary leader = result.members().getFirst();
            assertTrue(leader.householdLeader(), "initial household member should be a leader");
            assertEquals(HouseholdMemberStatus.ACTIVE, leader.status());

            verify(householdRepository).save(any(Household.class));
        }

        @Test
        void givenNullHouseholdName_thenUsesDefaultName() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizeHouseholdCommand command = new OrganizeHouseholdCommand(
                    partyLeaderId,
                    null
            );

            ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
            when(householdRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            service.organize(command);

            assertEquals("New Household", captor.getValue().name());
            verify(householdRepository).save(any(Household.class));
        }

        @Test
        void givenNullHouseholdLeaderId_thenThrows() {
            assertThrows(NullPointerException.class, () -> new OrganizeHouseholdCommand(
                    null,
                    "My Household"
            ));
            verify(householdRepository, never()).save(any());
        }

        @Test
        void givenHouseholdLeaderAlreadyBelongsToHousehold_thenThrowsHouseholdAlreadyExistsException() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household existingHousehold = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findByUserId(partyLeaderId)).thenReturn(Optional.of(existingHousehold));

            OrganizeHouseholdCommand command = new OrganizeHouseholdCommand(
                    partyLeaderId,
                    "Trip Budget"
            );

            assertThrows(HouseholdAlreadyExistsException.class, () -> service.organize(command));

            verify(householdRepository, never()).save(any());
        }

        @Test
        void givenIncomingPendingInvitations_thenCancelsAllBeforeCreatingHousehold() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizeHouseholdCommand command = new OrganizeHouseholdCommand(
                    partyLeaderId,
                    "Trip Budget"
            );

            Invitation incoming1 = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(partyLeaderId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(null)
                    .build();
            Invitation incoming2 = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(partyLeaderId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(null)
                    .build();

            when(invitationRepository.findByInviteeUserId(partyLeaderId)).thenReturn(List.of(incoming1, incoming2));
            ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
            when(householdRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            service.organize(command);

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository, times(2)).save(invitationCaptor.capture());
            List<Invitation> cancelled = invitationCaptor.getAllValues();
            assertEquals(2, cancelled.size());
            assertTrue(cancelled.stream().allMatch(i -> i.status() == InvitationStatus.CANCELLED));

            verify(householdRepository).save(any(Household.class));
        }

        @Test
        void givenHouseholdMemberAlreadyBelongsToHousehold_thenThrowsHouseholdAlreadyExistsException() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household existingHousehold = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .userId(memberId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findByUserId(memberId)).thenReturn(Optional.of(existingHousehold));

            OrganizeHouseholdCommand command = new OrganizeHouseholdCommand(
                    memberId,
                    "Trip Budget"
            );

            assertThrows(HouseholdAlreadyExistsException.class, () -> service.organize(command));

            verify(householdRepository, never()).save(any());
        }
    }

    @Nested
    class GetHouseholdMembersUserId {

        @Test
        void givenUserId_thenDelegatesToRepositoryAndReturnsResult() {
            UUID userId = UUID.randomUUID();
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            List<UUID> expected = List.of(memberId1, memberId2);

            when(householdRepository.findMembersUserIdsByUserId(userId)).thenReturn(expected);

            List<UUID> result = service.getHouseholdMembersUserIds(userId);

            assertEquals(expected, result, "result should be returned as-is from repository");
            verify(householdRepository).findMembersUserIdsByUserId(userId);
        }

        @Test
        void givenRepositoryReturnsEmptyList_thenReturnUserIdFromParam() {
            UUID userId = UUID.randomUUID();

            when(householdRepository.findMembersUserIdsByUserId(userId)).thenReturn(List.of());

            List<UUID> result = service.getHouseholdMembersUserIds(userId);

            assertNotNull(result);
            assertFalse(result.isEmpty(), "result should not be empty.");
            verify(householdRepository).findMembersUserIdsByUserId(userId);
        }
    }

    @Nested
    class RetrieveByUserId {

        @Test
        void givenHouseholdWithMembers_thenMapsHouseholdMemberSummariesWithDailyAverage() {
            UUID userId = UUID.randomUUID();
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .userId(memberId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.retrieveByUserId(userId)).thenReturn(List.of(household));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(partyLeaderId).firstName("Ada").lastName("Lovelace").build(),
                    User.builder().id(memberId).firstName("Alan").lastName("Turing").build()
            ));
            when(transactionRepository.getDailyAveragePastWeek(any())).thenReturn(List.of(
                    new SummaryPoint(partyLeaderId.toString(), 3.5),
                    new SummaryPoint(memberId.toString(), 1.0)
            ));

            List<HouseholdSummary> result = service.retrieveByUserId(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
            HouseholdSummary summary = result.getFirst();
            assertEquals(householdId, summary.id());
            assertEquals(2, summary.members().size());

            HouseholdMemberSummary leaderSummary = summary.members().stream()
                    .filter(HouseholdMemberSummary::householdLeader)
                    .findFirst()
                    .orElseThrow();
            assertEquals(partyLeaderId, leaderSummary.user().id());
            assertEquals("Ada Lovelace", leaderSummary.user().fullName());
            assertEquals("AL", leaderSummary.user().initial());

            HouseholdMemberSummary memberSummary = summary.members().stream()
                    .filter(s -> !s.householdLeader())
                    .findFirst()
                    .orElseThrow();
            assertEquals(memberId, memberSummary.user().id());
            assertEquals("Alan Turing", memberSummary.user().fullName());
            assertEquals("AT", memberSummary.user().initial());

            verify(householdRepository).retrieveByUserId(userId);
            verify(userClient).getUsersByIds(List.of(partyLeaderId, memberId));
            verify(transactionRepository).getDailyAveragePastWeek(Set.of(partyLeaderId, memberId));
        }

        @Test
        void givenNoHouseholds_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(householdRepository.retrieveByUserId(userId)).thenReturn(List.of());

            List<HouseholdSummary> result = service.retrieveByUserId(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(householdRepository).retrieveByUserId(userId);
            verify(transactionRepository, never()).getDailyAveragePastWeek(any());
        }
    }

    @Nested
    class RemoveHouseholdMember {

        @Test
        void givenHouseholdLeaderKicksHouseholdMember_thenHouseholdMemberIsKicked() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .id(partyLeaderId)
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .id(participantId)
                                    .userId(participantId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
            when(householdRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeMember(householdId, participantId, partyLeaderId);

            ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
            verify(householdRepository).save(captor.capture());
            Household saved = captor.getValue();
            assertEquals(1, saved.members().size());
            assertEquals(partyLeaderId, saved.members().getFirst().userId());
            assertTrue(saved.members().stream().noneMatch(p -> p.userId().equals(participantId)));
        }

        @Test
        void givenHouseholdMemberKickThemselves_thenHouseholdMemberIsRemoved() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .id(partyLeaderId)
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .id(participantId)
                                    .userId(participantId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
            when(householdRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeMember(householdId, participantId, participantId);

            ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
            verify(householdRepository).save(captor.capture());
            Household saved = captor.getValue();
            assertEquals(1, saved.members().size());
            assertEquals(partyLeaderId, saved.members().getFirst().userId());
        }

        @Test
        void givenHouseholdLeaderKickThemselves_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

            assertThrows(CannotRemoveOwnerException.class, () -> service.removeMember(householdId, partyLeaderId, partyLeaderId));
            verify(householdRepository, never()).save(any());
        }

        @Test
        void givenHouseholdMemberKicksLeader_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            HouseholdMember.builder()
                                    .userId(memberId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

            assertThrows(ForbiddenException.class, () -> service.removeMember(householdId, partyLeaderId, memberId));
            verify(householdRepository, never()).save(any());
        }
    }

    @Nested
    class DisbandHousehold {

        @Test
        void givenHouseholdLeaderDisbandsHousehold_thenDeleteByIdIsCalled() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

            service.disbandHousehold(householdId, partyLeaderId);

            verify(householdRepository).deleteById(householdId);
        }

        @Test
        void givenHouseholdNotFound_thenThrows() {
            UUID householdId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

            assertThrows(HouseholdNotFoundException.class, () -> service.disbandHousehold(householdId, requesterId));
            verify(householdRepository, never()).deleteById(any());
        }

        @Test
        void givenNotLeaderOrHouseholdMember_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID otherPlayerId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

            assertThrows(NotHouseholdLeaderException.class, () -> service.disbandHousehold(householdId, otherPlayerId));
            verify(householdRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PatchHousehold {

        @Test
        void givenOwnerUpdatesName_thenNameIsUpdated() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchHouseholdCommand command = PatchHouseholdCommand.builder()
                    .id(householdId)
                    .householdName("Updated Budget")
                    .playerId(partyLeaderId)
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
            when(householdRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Household updated = service.patchHousehold(command);

            assertEquals("Updated Budget", updated.name(), "name should be updated");
            verify(householdRepository).save(any(Household.class));
        }

        @Test
        void givenHouseholdNotFound_thenThrows() {
            UUID householdId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            PatchHouseholdCommand command = PatchHouseholdCommand.builder()
                    .id(householdId)
                    .householdName("Updated Budget")
                    .playerId(requesterId)
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

            assertThrows(HouseholdNotFoundException.class, () -> service.patchHousehold(command));
            verify(householdRepository, never()).save(any());
        }

        @Test
        void givenNotHouseholdLeader_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID otherPlayerId = UUID.randomUUID();
            UUID householdId = UUID.randomUUID();
            Household household = Household.builder()
                    .id(householdId)
                    .name("Family Budget")
                    .leaderId(partyLeaderId)
                    .members(new ArrayList<>(List.of(
                            HouseholdMember.builder()
                                    .userId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(HouseholdMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchHouseholdCommand command = PatchHouseholdCommand.builder()
                    .id(householdId)
                    .householdName("Updated Budget")
                    .playerId(otherPlayerId)
                    .build();

            when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

            assertThrows(NotHouseholdLeaderException.class, () -> service.patchHousehold(command));
            verify(householdRepository, never()).save(any());
        }
    }
}
