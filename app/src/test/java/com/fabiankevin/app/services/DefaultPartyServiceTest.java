package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.NotPartyLeaderException;
import com.fabiankevin.app.exceptions.party.PartyNotFoundException;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.party.*;
import com.fabiankevin.app.models.party.*;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.persistence.PartyRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.party.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPartyServiceTest {
    @Mock
    private PartyRepository partyRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultPartyService service;

    @Nested
    class OrganizeParty {
        @Test
        void givenValidCommand_thenCreatesPartyWithOwnerAsPartyMember() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizePartyCommand command = new OrganizePartyCommand(
                    partyLeaderId,
                    "Trip Budget",
                    SharingMode.EVEN_SHARE
            );

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            when(partyRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            PartySummary result = service.organize(command);

            assertNotNull(result);
            assertEquals("Trip Budget", result.name());
            assertEquals(partyLeaderId, result.partyLeaderId());
            assertEquals(SharingMode.EVEN_SHARE, result.sharingMode());
            assertTrue(result.active());
            assertEquals(1, result.partyMembers().size());

            PartyMemberSummary leader = result.partyMembers().getFirst();
            assertTrue(leader.partyLeader(), "initial party member should be a leader");
            assertFalse(leader.partyMember(), "initial party member should not be a member");
            assertEquals(AccessLevel.VIEW_ONLY, leader.accessLevel());
            assertEquals(PartyMemberStatus.ACTIVE, leader.status());

            assertEquals(2, result.sharedItems().size());
            assertEquals(ResourceType.TRANSACTION, result.sharedItems().get(0).type());
            assertEquals(ResourceType.CHECKLIST, result.sharedItems().get(1).type());

            verify(partyRepository).save(any(Party.class));
        }

        @Test
        void givenNullPartyName_thenUsesDefaultName() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizePartyCommand command = new OrganizePartyCommand(
                    partyLeaderId,
                    null,
                    SharingMode.EVEN_SHARE
            );

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            when(partyRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            service.organize(command);

            assertEquals("New Party", captor.getValue().name());
            verify(partyRepository).save(any(Party.class));
        }

        @Test
        void givenNullPartyLeaderId_thenThrows() {
            assertThrows(NullPointerException.class, () -> new OrganizePartyCommand(
                    null,
                    "My Party",
                    SharingMode.EVEN_SHARE
            ));
            verify(partyRepository, never()).save(any());
        }

        @Test
        void givenPartyLeaderAlreadyBelongsToParty_thenReturnsExistingParty() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party existingParty = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            when(partyRepository.findByPlayerId(partyLeaderId)).thenReturn(Optional.of(existingParty));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(partyLeaderId).firstName("Ada").lastName("Lovelace").build()
            ));

            OrganizePartyCommand command = new OrganizePartyCommand(
                    partyLeaderId,
                    "Trip Budget",
                    SharingMode.EVEN_SHARE
            );
            PartySummary result = service.organize(command);

            assertNotNull(result);
            assertEquals(partyId, result.id());
            assertEquals("Family Budget", result.name());
            assertEquals(partyLeaderId, result.partyLeaderId());
            assertEquals(SharingMode.EVEN_SHARE, result.sharingMode());
            verify(partyRepository, never()).save(any());
        }

        @Test
        void givenIncomingPendingInvitations_thenCancelsAllBeforeCreatingParty() {
            UUID partyLeaderId = UUID.randomUUID();
            OrganizePartyCommand command = new OrganizePartyCommand(
                    partyLeaderId,
                    "Trip Budget",
                    SharingMode.EVEN_SHARE
            );

            Invitation incoming1 = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterPlayerId(UUID.randomUUID())
                    .inviteePlayerId(partyLeaderId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .partyId(null)
                    .build();
            Invitation incoming2 = Invitation.builder()
                    .id(UUID.randomUUID())
                    .inviterPlayerId(UUID.randomUUID())
                    .inviteePlayerId(partyLeaderId)
                    .proposedSharingMode(SharingMode.EVEN_SHARE)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .partyId(null)
                    .build();

            when(invitationRepository.findByInviteeUserId(partyLeaderId)).thenReturn(List.of(incoming1, incoming2));
            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            when(partyRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.getUsersByIds(any())).thenReturn(List.of());

            service.organize(command);

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository, times(2)).save(invitationCaptor.capture());
            List<Invitation> cancelled = invitationCaptor.getAllValues();
            assertEquals(2, cancelled.size());
            assertTrue(cancelled.stream().allMatch(i -> i.status() == InvitationStatus.CANCELLED));

            verify(partyRepository).save(any(Party.class));
        }

        @Test
        void givenPartyMemberAlreadyBelongsToParty_thenReturnsExistingParty() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party existingParty = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(memberId)
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

            when(partyRepository.findByPlayerId(memberId)).thenReturn(Optional.of(existingParty));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(partyLeaderId).firstName("Ada").lastName("Lovelace").build(),
                    User.builder().id(memberId).firstName("Alan").lastName("Turing").build()
            ));

            OrganizePartyCommand command = new OrganizePartyCommand(
                    memberId,
                    "Trip Budget",
                    SharingMode.EVEN_SHARE
            );
            PartySummary result = service.organize(command);

            assertNotNull(result);
            assertEquals(partyId, result.id());
            assertEquals("Family Budget", result.name());
            assertEquals(partyLeaderId, result.partyLeaderId());
            assertEquals(SharingMode.EVEN_SHARE, result.sharingMode());
            verify(partyRepository, never()).save(any());
        }
    }

    @Nested
    class GetPartyMembersUserId {

        @Test
        void givenUserId_thenDelegatesToRepositoryAndReturnsResult() {
            UUID userId = UUID.randomUUID();
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            List<UUID> expected = List.of(memberId1, memberId2);

            when(partyRepository.findPartyMembersPlayerIdsByPlayerId(userId)).thenReturn(expected);

            List<UUID> result = service.getPartyMembersUserId(userId);

            assertEquals(expected, result, "result should be returned as-is from repository");
            verify(partyRepository).findPartyMembersPlayerIdsByPlayerId(userId);
        }

        @Test
        void givenRepositoryReturnsEmptyList_thenReturnUserIdFromParam() {
            UUID userId = UUID.randomUUID();

            when(partyRepository.findPartyMembersPlayerIdsByPlayerId(userId)).thenReturn(List.of());

            List<UUID> result = service.getPartyMembersUserId(userId);

            assertNotNull(result);
            assertFalse(result.isEmpty(), "result should not be empty.");
            verify(partyRepository).findPartyMembersPlayerIdsByPlayerId(userId);
        }
    }

    @Nested
    class RetrieveByUserId {

        @Test
        void givenPartyWithMembers_thenMapsPartyMemberSummariesWithDailyAverage() {
            UUID userId = UUID.randomUUID();
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(memberId)
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

            when(partyRepository.retrieveByPlayerId(userId)).thenReturn(List.of(party));
            when(userClient.getUsersByIds(any())).thenReturn(List.of(
                    User.builder().id(partyLeaderId).firstName("Ada").lastName("Lovelace").build(),
                    User.builder().id(memberId).firstName("Alan").lastName("Turing").build()
            ));
            when(transactionRepository.getDailyAveragePastWeek(any())).thenReturn(List.of(
                    new SummaryPoint(partyLeaderId.toString(), 3.5),
                    new SummaryPoint(memberId.toString(), 1.0)
            ));

            List<PartySummary> result = service.retrieveByUserId(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
            PartySummary summary = result.getFirst();
            assertEquals(partyId, summary.id());
            assertEquals(2, summary.partyMembers().size());

            PartyMemberSummary leaderSummary = summary.partyMembers().stream()
                    .filter(PartyMemberSummary::partyLeader)
                    .findFirst()
                    .orElseThrow();
            assertEquals(partyLeaderId, leaderSummary.playerId());
            assertEquals("Ada Lovelace", leaderSummary.name());
            assertEquals("AL", leaderSummary.initial());
            assertEquals(3.5, leaderSummary.pastWeekDailyAverageTransactionCount());

            PartyMemberSummary memberSummary = summary.partyMembers().stream()
                    .filter(s -> !s.partyLeader())
                    .findFirst()
                    .orElseThrow();
            assertEquals(memberId, memberSummary.playerId());
            assertEquals("Alan Turing", memberSummary.name());
            assertEquals("AT", memberSummary.initial());
            assertEquals(1.0, memberSummary.pastWeekDailyAverageTransactionCount());

            verify(partyRepository).retrieveByPlayerId(userId);
            verify(userClient).getUsersByIds(List.of(partyLeaderId, memberId));
            verify(transactionRepository).getDailyAveragePastWeek(Set.of(partyLeaderId, memberId));
        }

        @Test
        void givenNoParties_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(partyRepository.retrieveByPlayerId(userId)).thenReturn(List.of());

            List<PartySummary> result = service.retrieveByUserId(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(partyRepository).retrieveByPlayerId(userId);
            verify(transactionRepository, never()).getDailyAveragePastWeek(any());
        }
    }

    @Nested
    class KickPartyMember {

        @Test
        void givenPartyLeaderKicksPartyMember_thenPartyMemberIsKicked() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(participantId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
            when(partyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.kickPartyMember(partyId, participantId, partyLeaderId);

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            verify(partyRepository).save(captor.capture());
            Party saved = captor.getValue();
            assertEquals(1, saved.partyMembers().size());
            assertEquals(partyLeaderId, saved.partyMembers().getFirst().playerId());
            assertTrue(saved.partyMembers().stream().noneMatch(p -> p.playerId().equals(participantId)));
        }

        @Test
        void givenPartyMemberKickThemselves_thenPartyMemberIsRemoved() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(participantId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
            when(partyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.kickPartyMember(partyId, participantId, participantId);

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            verify(partyRepository).save(captor.capture());
            Party saved = captor.getValue();
            assertEquals(1, saved.partyMembers().size());
            assertEquals(partyLeaderId, saved.partyMembers().getFirst().playerId());
        }

        @Test
        void givenPartyLeaderKickThemselves_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));

            assertThrows(CannotRemoveOwnerException.class, () -> service.kickPartyMember(partyId, partyLeaderId, partyLeaderId));
            verify(partyRepository, never()).save(any());
        }

        @Test
        void givenPartyMemberKicksLeader_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(PartyMemberStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            PartyMember.builder()
                                    .playerId(memberId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));

            assertThrows(ForbiddenException.class, () -> service.kickPartyMember(partyId, partyLeaderId, memberId));
            verify(partyRepository, never()).save(any());
        }
    }

    @Nested
    class DisbandParty {

        @Test
        void givenPartyLeaderDisbandsParty_thenDeleteByIdIsCalled() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));

            service.disbandParty(partyId, partyLeaderId);

            verify(partyRepository).deleteById(partyId);
        }

        @Test
        void givenPartyNotFound_thenThrows() {
            UUID partyId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            when(partyRepository.findById(partyId)).thenReturn(Optional.empty());

            assertThrows(PartyNotFoundException.class, () -> service.disbandParty(partyId, requesterId));
            verify(partyRepository, never()).deleteById(any());
        }

        @Test
        void givenNotLeaderOrPartyMember_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID otherPlayerId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));

            assertThrows(NotPartyLeaderException.class, () -> service.disbandParty(partyId, otherPlayerId));
            verify(partyRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PatchParty {

        @Test
        void givenOwnerUpdatesName_thenNameIsUpdated() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(partyId)
                    .partyName("Updated Budget")
                    .playerId(partyLeaderId)
                    .build();

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
            when(partyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Party updated = service.patchParty(command);

            assertEquals("Updated Budget", updated.name(), "name should be updated");
            assertEquals(SharingMode.EVEN_SHARE, updated.sharingMode(), "sharingMode should remain unchanged");
            verify(partyRepository).save(any(Party.class));
        }

        @Test
        void givenPartyNotFound_thenThrows() {
            UUID partyId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(partyId)
                    .partyName("Updated Budget")
                    .playerId(requesterId)
                    .build();

            when(partyRepository.findById(partyId)).thenReturn(Optional.empty());

            assertThrows(PartyNotFoundException.class, () -> service.patchParty(command));
            verify(partyRepository, never()).save(any());
        }

        @Test
        void givenNotPartyLeader_thenThrows() {
            UUID partyLeaderId = UUID.randomUUID();
            UUID otherPlayerId = UUID.randomUUID();
            UUID partyId = UUID.randomUUID();
            Party party = Party.builder()
                    .id(partyId)
                    .name("Family Budget")
                    .partyLeaderId(partyLeaderId)
                    .partyMembers(new ArrayList<>(List.of(
                            PartyMember.builder()
                                    .playerId(partyLeaderId)
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

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(partyId)
                    .partyName("Updated Budget")
                    .playerId(otherPlayerId)
                    .build();

            when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));

            assertThrows(NotPartyLeaderException.class, () -> service.patchParty(command));
            verify(partyRepository, never()).save(any());
        }
    }
}
