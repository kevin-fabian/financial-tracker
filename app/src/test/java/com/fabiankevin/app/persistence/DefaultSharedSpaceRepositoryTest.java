package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSharedSpaceRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(DefaultSharedSpaceRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultSharedSpaceRepositoryTest {

    @MockitoSpyBean
    private JpaSharedSpaceRepository jpaSharedSpaceRepository;

    @Autowired
    private SharedSpaceRepository sharedSpaceRepository;

    private SharedSpace sharedSpace;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public SharedSpaceRepository sharedSpaceRepository(JpaSharedSpaceRepository jpaSharedSpaceRepository) {
            return new DefaultSharedSpaceRepository(jpaSharedSpaceRepository);
        }
    }

    @BeforeEach
    void setUp() {
        UUID ownerUserId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();

        SharedResource resource = SharedResource.builder()
                .type(ResourceType.TRANSACTION)
                .items(List.of("txn-001", "txn-002"))
                .sharedAt(Instant.now())
                .build();

        SpaceParticipant participant = SpaceParticipant.builder()
                .userId(participantUserId)
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        sharedSpace = SharedSpace.builder()
                .spaceName("Family 2026 Budget")
                .ownerUserId(ownerUserId)
                .participants(List.of(participant))
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .sharedResources(List.of(resource))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenNewSharedSpace_shouldPersistAndRetrieveAllFields() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> optRetrievedSharedSpace = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(optRetrievedSharedSpace).isPresent();
        SharedSpace retrievedsharedSpace = optRetrievedSharedSpace.get();
        Assertions.assertThat(retrievedsharedSpace.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(retrievedsharedSpace.spaceName()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(retrievedsharedSpace.ownerUserId()).isEqualTo(sharedSpace.ownerUserId());
        Assertions.assertThat(retrievedsharedSpace.sharingMode()).isEqualTo(SharingMode.MUTUAL_SHARING);
        Assertions.assertThat(retrievedsharedSpace.active()).isTrue();
        Assertions.assertThat(retrievedsharedSpace.createdAt()).isEqualTo(sharedSpace.createdAt());
        Assertions.assertThat(retrievedsharedSpace.updatedAt()).isEqualTo(sharedSpace.updatedAt());

        Assertions.assertThat(retrievedsharedSpace.participants())
                .as("participants should be persisted and retrieved")
                .hasSize(1);
        SpaceParticipant retrievedParticipant = retrievedsharedSpace.participants().getFirst();
        Assertions.assertThat(retrievedParticipant.id()).as("participant id should be generated").isNotNull();
        Assertions.assertThat(retrievedParticipant.userId())
                .isEqualTo(sharedSpace.participants().getFirst().userId());
        Assertions.assertThat(retrievedParticipant.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
        Assertions.assertThat(retrievedParticipant.status()).isEqualTo(ParticipantStatus.ACTIVE);
        Assertions.assertThat(retrievedParticipant.joinedAt())
                .isEqualTo(sharedSpace.participants().getFirst().joinedAt());
        Assertions.assertThat(retrievedsharedSpace.sharedResources())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        SharedResource retrievedSharedResource = retrievedsharedSpace.sharedResources().getFirst();
        Assertions.assertThat(retrievedSharedResource.id()).as("resource id should be generated").isNotNull();
        Assertions.assertThat(retrievedSharedResource.type()).isEqualTo(ResourceType.TRANSACTION);
        Assertions.assertThat(retrievedSharedResource.items()).containsExactly("txn-001", "txn-002");
        Assertions.assertThat(retrievedSharedResource.sharedAt())
                .isEqualTo(sharedSpace.sharedResources().getFirst().sharedAt());

        verify(jpaSharedSpaceRepository, times(1)).save(any());
        verify(jpaSharedSpaceRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenSharedSpaceWithParticipants_shouldCascadePersist() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().participants())
                .as("participants should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().participants().getFirst().userId())
                .isEqualTo(sharedSpace.participants().getFirst().userId());
        Assertions.assertThat(found.get().participants().getFirst().accessLevel())
                .isEqualTo(AccessLevel.READ_WRITE);
    }

    @Test
    void save_givenSharedSpaceWithSharedResources_shouldCascadePersist() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().sharedResources())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().sharedResources().getFirst().items())
                .as("resource item ids should persist")
                .containsExactly("txn-001", "txn-002");
        Assertions.assertThat(found.get().sharedResources().getFirst().type())
                .isEqualTo(ResourceType.TRANSACTION);
    }

    @Test
    void save_givenSharedSpaceWithNullLists_shouldPersistWithEmptyLists() {
        SharedSpace minimal = SharedSpace.builder()
                .spaceName("Empty Space")
                .ownerUserId(UUID.randomUUID())
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SharedSpace saved = sharedSpaceRepository.save(minimal);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().participants()).isEmpty();
        Assertions.assertThat(found.get().sharedResources()).isEmpty();
    }

    @Test
    void findById_givenExistingSharedSpace_shouldReturnSharedSpace() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().spaceName()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(found.get().sharingMode()).isEqualTo(SharingMode.MUTUAL_SHARING);

        verify(jpaSharedSpaceRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        Optional<SharedSpace> found = sharedSpaceRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void findByUserId_givenUserIsOwner_shouldReturnSharedSpace() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findByUserId(saved.ownerUserId());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(saved.ownerUserId());
    }

    @Test
    void findByUserId_givenUserIsParticipant_shouldReturnSharedSpace() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);
        UUID participantUserId = saved.participants().getFirst().userId();

        Optional<SharedSpace> found = sharedSpaceRepository.findByUserId(participantUserId);

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(participantUserId);
    }

    @Test
    void findByUserId_givenUserIsNotOwnerOrParticipant_shouldReturnEmpty() {
        Optional<SharedSpace> found = sharedSpaceRepository.findByUserId(UUID.randomUUID());

        Assertions.assertThat(found).isEmpty();

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(any());
    }
}
