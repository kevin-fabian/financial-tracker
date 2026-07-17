package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.PartyMember;
import com.fabiankevin.app.models.shared_space.SharedItem;
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

@Import(DefaultPartyRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultPartyRepositoryTest {

    @MockitoSpyBean
    private JpaSharedSpaceRepository jpaSharedSpaceRepository;

    @Autowired
    private SharedSpaceRepository sharedSpaceRepository;

    private Party party;

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

        SharedItem resource = SharedItem.builder()
                .type(ResourceType.TRANSACTION)
                .items(List.of("txn-001", "txn-002"))
                .sharedAt(Instant.now())
                .build();

        PartyMember participant = PartyMember.builder()
                .playerId(participantUserId)
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        party = Party.builder()
                .name("Family 2026 Budget")
                .partyLeaderId(ownerUserId)
                .partyMembers(List.of(participant))
                .sharingMode(SharingMode.EVEN_SHARE)
                .sharedItems(List.of(resource))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenNewSharedSpace_shouldPersistAndRetrieveAllFields() {
        Party saved = sharedSpaceRepository.save(party);

        Optional<Party> optRetrievedSharedSpace = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(optRetrievedSharedSpace).isPresent();
        Party retrievedsharedSpace = optRetrievedSharedSpace.get();
        Assertions.assertThat(retrievedsharedSpace.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(retrievedsharedSpace.name()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(retrievedsharedSpace.partyLeaderId()).isEqualTo(party.partyLeaderId());
        Assertions.assertThat(retrievedsharedSpace.sharingMode()).isEqualTo(SharingMode.EVEN_SHARE);
        Assertions.assertThat(retrievedsharedSpace.active()).isTrue();
        Assertions.assertThat(retrievedsharedSpace.createdAt()).isEqualTo(party.createdAt());
        Assertions.assertThat(retrievedsharedSpace.updatedAt()).isEqualTo(party.updatedAt());

        Assertions.assertThat(retrievedsharedSpace.partyMembers())
                .as("partyMembers should be persisted and retrieved")
                .hasSize(1);
        PartyMember retrievedParticipant = retrievedsharedSpace.partyMembers().getFirst();
        Assertions.assertThat(retrievedParticipant.id()).as("participant id should be generated").isNotNull();
        Assertions.assertThat(retrievedParticipant.playerId())
                .isEqualTo(party.partyMembers().getFirst().playerId());
        Assertions.assertThat(retrievedParticipant.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
        Assertions.assertThat(retrievedParticipant.status()).isEqualTo(ParticipantStatus.ACTIVE);
        Assertions.assertThat(retrievedParticipant.joinedAt())
                .isEqualTo(party.partyMembers().getFirst().joinedAt());
        Assertions.assertThat(retrievedsharedSpace.sharedItems())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        SharedItem retrievedSharedItem = retrievedsharedSpace.sharedItems().getFirst();
        Assertions.assertThat(retrievedSharedItem.id()).as("resource id should be generated").isNotNull();
        Assertions.assertThat(retrievedSharedItem.type()).isEqualTo(ResourceType.TRANSACTION);
        Assertions.assertThat(retrievedSharedItem.items()).containsExactly("txn-001", "txn-002");
        Assertions.assertThat(retrievedSharedItem.sharedAt())
                .isEqualTo(party.sharedItems().getFirst().sharedAt());

        verify(jpaSharedSpaceRepository, times(1)).save(any());
        verify(jpaSharedSpaceRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenSharedSpaceWithParticipants_shouldCascadePersist() {
        Party saved = sharedSpaceRepository.save(party);

        Optional<Party> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().partyMembers())
                .as("partyMembers should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().partyMembers().getFirst().playerId())
                .isEqualTo(party.partyMembers().getFirst().playerId());
        Assertions.assertThat(found.get().partyMembers().getFirst().accessLevel())
                .isEqualTo(AccessLevel.READ_WRITE);
    }

    @Test
    void save_givenSharedSpaceWithSharedResources_shouldCascadePersist() {
        Party saved = sharedSpaceRepository.save(party);

        Optional<Party> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().sharedItems())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().sharedItems().getFirst().items())
                .as("resource item ids should persist")
                .containsExactly("txn-001", "txn-002");
        Assertions.assertThat(found.get().sharedItems().getFirst().type())
                .isEqualTo(ResourceType.TRANSACTION);
    }

    @Test
    void save_givenSharedSpaceWithNullLists_shouldPersistWithEmptyLists() {
        Party minimal = Party.builder()
                .name("Empty Space")
                .partyLeaderId(UUID.randomUUID())
                .sharingMode(SharingMode.EVEN_SHARE)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Party saved = sharedSpaceRepository.save(minimal);

        Optional<Party> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().partyMembers()).isEmpty();
        Assertions.assertThat(found.get().sharedItems()).isEmpty();
    }

    @Test
    void findById_givenExistingSharedSpace_shouldReturnSharedSpace() {
        Party saved = sharedSpaceRepository.save(party);

        Optional<Party> found = sharedSpaceRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().name()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(found.get().sharingMode()).isEqualTo(SharingMode.EVEN_SHARE);

        verify(jpaSharedSpaceRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        Optional<Party> found = sharedSpaceRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void findByUserId_givenUserIsOwner_shouldReturnSharedSpace() {
        Party saved = sharedSpaceRepository.save(party);

        Optional<Party> found = sharedSpaceRepository.findByUserId(saved.partyLeaderId());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(saved.partyLeaderId());
    }

    @Test
    void findByUserId_givenUserIsParticipant_shouldReturnSharedSpace() {
        Party saved = sharedSpaceRepository.save(party);
        UUID participantUserId = saved.partyMembers().getFirst().playerId();

        Optional<Party> found = sharedSpaceRepository.findByUserId(participantUserId);

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(participantUserId);
    }

    @Test
    void findByUserId_givenUserIsNotOwnerOrParticipant_shouldReturnEmpty() {
        Optional<Party> found = sharedSpaceRepository.findByUserId(UUID.randomUUID());

        Assertions.assertThat(found).isEmpty();

        verify(jpaSharedSpaceRepository, times(1)).findByUserId(any());
    }
}
