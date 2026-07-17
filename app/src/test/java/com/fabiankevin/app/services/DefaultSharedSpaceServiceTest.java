package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.ForbiddenException;
import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.exceptions.shared_space.SharedSpaceNotFoundException;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.SharedSpaceRepository;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
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
class DefaultSharedSpaceServiceTest {

    @Mock
    private SharedSpaceRepository spaceRepository;

    @InjectMocks
    private DefaultSharedSpaceService service;

    @Nested
    class CreateShare {

        @Test
        void givenValidCommand_thenCreatesSpaceWithOwnerAsParticipant() {
            UUID ownerUserId = UUID.randomUUID();
            CreateSharedSpaceCommand command = new CreateSharedSpaceCommand(
                    ownerUserId,
                    "Trip Budget",
                    SharingMode.MUTUAL_SHARING
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

            assertEquals(3, result.sharedResources().size());
            assertEquals(ResourceType.TRANSACTION, result.sharedResources().get(0).type());
            assertEquals(ResourceType.BUDGET, result.sharedResources().get(1).type());
            assertEquals(ResourceType.BUDGET, result.sharedResources().get(2).type());

            verify(spaceRepository).save(any(SharedSpace.class));
        }

        @Test
        void givenNullSpaceName_thenUsesDefaultName() {
            UUID ownerUserId = UUID.randomUUID();
            CreateSharedSpaceCommand command = new CreateSharedSpaceCommand(
                    ownerUserId,
                    null,
                    SharingMode.MUTUAL_SHARING
            );

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            when(spaceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.createShare(command);

            assertEquals("Shared Space", captor.getValue().spaceName());
            verify(spaceRepository).save(any(SharedSpace.class));
        }

        @Test
        void givenNullOwnerUserId_thenThrows() {
            assertThrows(NullPointerException.class, () -> new CreateSharedSpaceCommand(
                    null,
                    "My Space",
                    SharingMode.MUTUAL_SHARING
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
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            SpaceParticipant.builder()
                                    .userId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
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

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeParticipant(spaceId, participantUserId, ownerUserId);

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            verify(spaceRepository).save(captor.capture());
            SharedSpace saved = captor.getValue();
            assertEquals(1, saved.participants().size());
            assertEquals(ownerUserId, saved.participants().getFirst().userId());
            assertTrue(saved.participants().stream().noneMatch(p -> p.userId().equals(participantUserId)));
        }

        @Test
        void givenParticipantRemovesThemselves_thenParticipantIsRemoved() {
            UUID ownerUserId = UUID.randomUUID();
            UUID participantUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            SpaceParticipant.builder()
                                    .userId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
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

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
            when(spaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.removeParticipant(spaceId, participantUserId, participantUserId);

            ArgumentCaptor<SharedSpace> captor = ArgumentCaptor.forClass(SharedSpace.class);
            verify(spaceRepository).save(captor.capture());
            SharedSpace saved = captor.getValue();
            assertEquals(1, saved.participants().size());
            assertEquals(ownerUserId, saved.participants().getFirst().userId());
        }

        @Test
        void givenNonOwnerNonSelfAttemptsToRemoveParticipant_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID participantUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
                                    .accessLevel(AccessLevel.READ_WRITE)
                                    .status(ParticipantStatus.ACTIVE)
                                    .joinedAt(Instant.now())
                                    .build(),
                            SpaceParticipant.builder()
                                    .userId(participantUserId)
                                    .accessLevel(AccessLevel.VIEW_ONLY)
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

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(ForbiddenException.class, () -> service.removeParticipant(spaceId, participantUserId, otherUserId));
            verify(spaceRepository, never()).save(any());
        }

        @Test
        void givenAttemptToRemoveOwner_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
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

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(CannotRemoveOwnerException.class, () -> service.removeParticipant(spaceId, ownerUserId, ownerUserId));
            verify(spaceRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteSharedSpace {

        @Test
        void givenOwnerDeletesSpace_thenDeleteByIdIsCalled() {
            UUID ownerUserId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
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
            SharedSpace space = SharedSpace.builder()
                    .id(spaceId)
                    .spaceName("Family Budget")
                    .ownerUserId(ownerUserId)
                    .participants(new ArrayList<>(List.of(
                            SpaceParticipant.builder()
                                    .userId(ownerUserId)
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

            when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

            assertThrows(NotSpaceOwnerException.class, () -> service.deleteSharedSpace(spaceId, otherUserId));
            verify(spaceRepository, never()).deleteById(any());
        }
    }
}
