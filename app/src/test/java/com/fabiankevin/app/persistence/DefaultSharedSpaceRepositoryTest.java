package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.AccessLevel;
import com.fabiankevin.app.models.enums.ParticipantStatus;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SharingRule;
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
                .resourceName("Family Checking")
                .ownerUserId(ownerUserId)
                .itemIds(List.of("txn-001", "txn-002"))
                .sharedByOwner(true)
                .sharedAt(Instant.now())
                .build();

        SpaceParticipant participant = SpaceParticipant.builder()
                .userId(participantUserId)
                .accessLevel(AccessLevel.READ_WRITE)
                .invitedByUserId(ownerUserId)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .sharingRule(SharingRule.MUTUAL_DEFAULT)
                .build();

        sharedSpace = SharedSpace.builder()
                .spaceName("Family 2026 Budget")
                .ownerUserId(ownerUserId)
                .participants(List.of(participant))
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .sharedResources(List.of(resource))
                .defaultSharingRule(SharingRule.MUTUAL_DEFAULT)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenNewSharedSpace_shouldPersistAndRetrieve() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        SharedSpace restored = found.get();
        Assertions.assertThat(restored.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(restored.spaceName()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(restored.ownerUserId()).isEqualTo(sharedSpace.ownerUserId());
        Assertions.assertThat(restored.sharingMode()).isEqualTo(SharingMode.MUTUAL_SHARING);
        Assertions.assertThat(restored.active()).isTrue();
        Assertions.assertThat(restored.createdAt()).isEqualTo(sharedSpace.createdAt());
        Assertions.assertThat(restored.updatedAt()).isEqualTo(sharedSpace.updatedAt());
        Assertions.assertThat(restored.expiresAt()).isNull();

        Assertions.assertThat(restored.defaultSharingRule())
                .as("default sharing rule should persist")
                .isNotNull();
        Assertions.assertThat(restored.defaultSharingRule().sharesOwnResources()).isTrue();
        Assertions.assertThat(restored.defaultSharingRule().visibleResourceTypes())
                .containsExactlyInAnyOrder(ResourceType.values());
        Assertions.assertThat(restored.defaultSharingRule().sharedResourceIds()).isEmpty();
        Assertions.assertThat(restored.defaultSharingRule().autoApproveUnder()).isNull();
        Assertions.assertThat(restored.defaultSharingRule().requiresApproval()).isFalse();

        Assertions.assertThat(restored.participants())
                .as("participants should be persisted and retrieved")
                .hasSize(1);
        SpaceParticipant restoredParticipant = restored.participants().getFirst();
        Assertions.assertThat(restoredParticipant.id()).as("participant id should be generated").isNotNull();
        Assertions.assertThat(restoredParticipant.userId())
                .isEqualTo(sharedSpace.participants().getFirst().userId());
        Assertions.assertThat(restoredParticipant.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
        Assertions.assertThat(restoredParticipant.invitedByUserId())
                .isEqualTo(sharedSpace.ownerUserId());
        Assertions.assertThat(restoredParticipant.status()).isEqualTo(ParticipantStatus.ACTIVE);
        Assertions.assertThat(restoredParticipant.joinedAt())
                .isEqualTo(sharedSpace.participants().getFirst().joinedAt());
        Assertions.assertThat(restoredParticipant.sharingRule())
                .as("participant sharing rule should persist")
                .isNotNull();
        Assertions.assertThat(restoredParticipant.sharingRule().sharesOwnResources()).isTrue();
        Assertions.assertThat(restoredParticipant.sharingRule().visibleResourceTypes())
                .containsExactlyInAnyOrder(ResourceType.values());
        Assertions.assertThat(restoredParticipant.sharingRule().sharedResourceIds()).isEmpty();
        Assertions.assertThat(restoredParticipant.sharingRule().autoApproveUnder()).isNull();
        Assertions.assertThat(restoredParticipant.sharingRule().requiresApproval()).isFalse();

        Assertions.assertThat(restored.sharedResources())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        SharedResource restoredResource = restored.sharedResources().getFirst();
        Assertions.assertThat(restoredResource.id()).as("resource id should be generated").isNotNull();
        Assertions.assertThat(restoredResource.type()).isEqualTo(ResourceType.TRANSACTION);
        Assertions.assertThat(restoredResource.resourceName()).isEqualTo("Family Checking");
        Assertions.assertThat(restoredResource.ownerUserId()).isEqualTo(sharedSpace.ownerUserId());
        Assertions.assertThat(restoredResource.itemIds()).containsExactly("txn-001", "txn-002");
        Assertions.assertThat(restoredResource.sharedByOwner()).isTrue();
        Assertions.assertThat(restoredResource.sharedAt())
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
        Assertions.assertThat(found.get().participants().getFirst().sharingRule())
                .as("participant sharing rule should persist")
                .isNotNull();
    }

    @Test
    void save_givenSharedSpaceWithSharedResources_shouldCascadePersist() {
        SharedSpace saved = sharedSpaceRepository.save(sharedSpace);

        Optional<SharedSpace> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().sharedResources())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().sharedResources().getFirst().itemIds())
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
                .sharingMode(SharingMode.OWNER_PROVIDES)
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
        Assertions.assertThat(found.get().defaultSharingRule())
                .as("default sharing rule should persist")
                .isNotNull();
        Assertions.assertThat(found.get().defaultSharingRule().sharesOwnResources()).isTrue();

        verify(jpaSharedSpaceRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        Optional<SharedSpace> found = sharedSpaceRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }
}
