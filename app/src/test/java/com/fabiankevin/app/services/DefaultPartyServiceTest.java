package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.ForbiddenException;
import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.exceptions.shared_space.SharedSpaceNotFoundException;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.Player;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;
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
    private SharedSpaceRepository spaceRepository;

    @InjectMocks
    private DefaultPartyService service;

    @Nested
    class CreateShare {

        @Test
        void givenValidCommand_thenCreatesSpaceWithOwnerAsParticipant() {
            UUID ownerUserId = UUID.randomUUID();
            OrganizePartyCommand command = new OrganizePartyCommand(
                    ownerUserId,
                    "Trip Budget",
                    SharingMode.EVEN_SHARE
            );

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            Party result = service.organize(command);

            assertNotNull(result);
            assertEquals("Trip Budget", result.name());
            assertEquals(ownerUserId, result.partyLeaderId());
            assertEquals(SharingMode.EVEN_SHARE, result.sharingMode());
            assertTrue(result.active());
            assertEquals(1, result.participants().size());

            Player owner = result.participants().getFirst();
            assertEquals(ownerUserId, owner.playerId());
            assertEquals(AccessLevel.READ_WRITE, owner.accessLevel());
            assertEquals(ParticipantStatus.ACTIVE, owner.status());

            assertEquals(3, result.sharedResources().size());
            assertEquals(ResourceType.TRANSACTION, result.sharedResources().get(0).type());
            assertEquals(ResourceType.BUDGET, result.sharedResources().get(1).type());
            assertEquals(ResourceType.BUDGET, result.sharedResources().get(2).type());

            verify(spaceRepository).save(any(Party.class));
        }

        @Test
        void givenNullSpaceName_thenUsesDefaultName() {
            UUID ownerUserId = UUID.randomUUID();
            OrganizePartyCommand command = new OrganizePartyCommand(
                    ownerUserId,
                    null,
                    SharingMode.EVEN_SHARE
            );

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.organize(command);

            assertEquals("Shared Space", captor.getValue().name());
            verify(spaceRepository).save(any(Party.class));
        }

        @Test
        void givenNullOwnerUserId_thenThrows() {
            assertThrows(NullPointerException.class, () -> new OrganizePartyCommand(
                    null,
                    "My Space",
                    SharingMode.EVEN_SHARE
            ));
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

    @Nested
    class RemoveParticipant {

        @Test
        void givenOwnerRemovesParticipant_thenParticipantIsRemoved() {
            UUID ownerUserId = UUID.randomUUID();
            UUID participantUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            Player.builder()
                                    .playerId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeParticipant(spaceId, participantUserId, ownerUserId);

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            verify(spaceRepository).save(captor.capture());
            Party saved = captor.getValue();
            assertEquals(1, saved.participants().size());
            assertEquals(ownerUserId, saved.participants().getFirst().playerId());
            assertTrue(saved.participants().stream().noneMatch(p -> p.playerId().equals(participantUserId)));
        }

        @Test
        void givenParticipantRemovesThemselves_thenParticipantIsRemoved() {
            UUID ownerUserId = UUID.randomUUID();
            UUID participantUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            Player.builder()
                                    .playerId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeParticipant(spaceId, participantUserId, participantUserId);

            ArgumentCaptor<Party> captor = ArgumentCaptor.forClass(Party.class);
            verify(spaceRepository).save(captor.capture());
            Party saved = captor.getValue();
            assertEquals(1, saved.participants().size());
            assertEquals(ownerUserId, saved.participants().getFirst().playerId());
        }

        @Test
        void givenNonOwnerNonSelfAttemptsToRemoveParticipant_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID participantUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            Player.builder()
                                    .playerId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(ForbiddenException.class, () -> service.removeParticipant(spaceId, participantUserId, otherUserId));
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenAttemptToRemoveOwner_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(CannotRemoveOwnerException.class, () -> service.removeParticipant(spaceId, ownerUserId, ownerUserId));
            verify(spaceRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteParty {

        @Test
        void givenOwnerDeletesSpace_thenDeleteByIdIsCalled() {
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            service.deleteSharedSpace(spaceId, ownerUserId);

            verify(spaceRepository).deleteById(spaceId);
        }

        @Test
        void givenSpaceNotFound_thenThrows() {
            UUID spaceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.empty());

            assertThrows(SharedSpaceNotFoundException.class, () -> service.deleteSharedSpace(spaceId, requesterId));
            verify(spaceRepository, never()).deleteById(any());
        }

        @Test
        void givenNonOwnerAttemptsToDelete_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(NotSpaceOwnerException.class, () -> service.deleteSharedSpace(spaceId, otherUserId));
            verify(spaceRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PatchParty {

        @Test
        void givenOwnerUpdatesName_thenNameIsUpdated() {
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(spaceId)
                    .partyName("Updated Budget")
                    .userId(ownerUserId)
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Party updated = service.patchSharedSpace(command);

            assertEquals("Updated Budget", updated.name(), "name should be updated");
            assertEquals(SharingMode.EVEN_SHARE, updated.sharingMode(), "sharingMode should remain unchanged");
            verify(spaceRepository).save(any(Party.class));
        }

        @Test
        void givenSpaceNotFound_thenThrows() {
            UUID spaceId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(spaceId)
                    .partyName("Updated Budget")
                    .userId(requesterId)
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.empty());

            assertThrows(SharedSpaceNotFoundException.class, () -> service.patchSharedSpace(command));
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenNonOwnerAttemptsToUpdate_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            Party space = Party.builder()
                    .id(spaceId)
                    .name("Family Budget")
                    .partyLeaderId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            Player.builder()
                                    .playerId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build()
                    )))
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .sharedResources(new ArrayList<>())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchPartyCommand command = PatchPartyCommand.builder()
                    .id(spaceId)
                    .partyName("Updated Budget")
                    .userId(otherUserId)
                    .build();

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(NotSpaceOwnerException.class, () -> service.patchSharedSpace(command));
            verify(spaceRepository, never()).save(any());
        }
    }
}
