package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.enums.party.ResourceType;
import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMember;
import com.fabiankevin.app.models.party.SharedItem;
import com.fabiankevin.app.persistence.jpa_repositories.JpaPartyRepository;
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
    private JpaPartyRepository jpaPartyRepository;

    @Autowired
    private PartyRepository partyRepository;

    private Party party;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public PartyRepository sharedSpaceRepository(JpaPartyRepository jpaPartyRepository) {
            return new DefaultPartyRepository(jpaPartyRepository);
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
                .status(PartyMemberStatus.ACTIVE)
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
    void save_givenNewParty_shouldPersistAndRetrieveAllFields() {
        Party saved = partyRepository.save(party);

        Optional<Party> optRetrievedSharedSpace = partyRepository.findById(saved.id());

        Assertions.assertThat(optRetrievedSharedSpace).isPresent();
        Party retrievedParty = optRetrievedSharedSpace.get();
        Assertions.assertThat(retrievedParty.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(retrievedParty.name()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(retrievedParty.partyLeaderId()).isEqualTo(party.partyLeaderId());
        Assertions.assertThat(retrievedParty.sharingMode()).isEqualTo(SharingMode.EVEN_SHARE);
        Assertions.assertThat(retrievedParty.active()).isTrue();
        Assertions.assertThat(retrievedParty.createdAt()).isEqualTo(party.createdAt());
        Assertions.assertThat(retrievedParty.updatedAt()).isEqualTo(party.updatedAt());

        Assertions.assertThat(retrievedParty.partyMembers())
                .as("partyMembers should be persisted and retrieved")
                .hasSize(1);
        PartyMember retrievePartyMember = retrievedParty.partyMembers().getFirst();
        Assertions.assertThat(retrievePartyMember.id()).as("participant id should be generated").isNotNull();
        Assertions.assertThat(retrievePartyMember.playerId())
                .isEqualTo(party.partyMembers().getFirst().playerId());
        Assertions.assertThat(retrievePartyMember.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
        Assertions.assertThat(retrievePartyMember.status()).isEqualTo(PartyMemberStatus.ACTIVE);
        Assertions.assertThat(retrievePartyMember.joinedAt())
                .isEqualTo(party.partyMembers().getFirst().joinedAt());
        Assertions.assertThat(retrievedParty.sharedItems())
                .as("shared resources should be persisted and retrieved")
                .hasSize(1);
        SharedItem retrievedshareditem = retrievedParty.sharedItems().getFirst();
        Assertions.assertThat(retrievedshareditem.id()).as("resource id should be generated").isNotNull();
        Assertions.assertThat(retrievedshareditem.type()).isEqualTo(ResourceType.TRANSACTION);
        Assertions.assertThat(retrievedshareditem.items()).containsExactly("txn-001", "txn-002");
        Assertions.assertThat(retrievedshareditem.sharedAt())
                .isEqualTo(party.sharedItems().getFirst().sharedAt());

        verify(jpaPartyRepository, times(1)).save(any());
        verify(jpaPartyRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenPartyWithPartyMember_shouldCascadePersist() {
        Party saved = partyRepository.save(party);

        Optional<Party> found = partyRepository.findById(saved.id());

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
    void save_givenPartyWithSharedResources_shouldCascadePersist() {
        Party saved = partyRepository.save(party);

        Optional<Party> found = partyRepository.findById(saved.id());

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
    void save_givenPartyWithNullLists_shouldPersistWithEmptyLists() {
        Party minimal = Party.builder()
                .name("Empty Space")
                .partyLeaderId(UUID.randomUUID())
                .sharingMode(SharingMode.EVEN_SHARE)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Party saved = partyRepository.save(minimal);

        Optional<Party> found = partyRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().partyMembers()).isEmpty();
        Assertions.assertThat(found.get().sharedItems()).isEmpty();
    }

    @Test
    void findById_givenExistingParty_shouldReturnExistingParty() {
        Party saved = partyRepository.save(party);

        Optional<Party> found = partyRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().name()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(found.get().sharingMode()).isEqualTo(SharingMode.EVEN_SHARE);

        verify(jpaPartyRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExistingParty_shouldReturnEmptyOptional() {
        Optional<Party> found = partyRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void findByUserId_givenPlayerIsOwner_shouldReturnParty() {
        Party saved = partyRepository.save(party);

        Optional<Party> found = partyRepository.findByPlayerId(saved.partyLeaderId());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaPartyRepository, times(1)).findByPlayerId(saved.partyLeaderId());
    }

    @Test
    void findByUserId_givenPlayerIsPartyMember_shouldReturnParty() {
        Party saved = partyRepository.save(party);
        UUID participantUserId = saved.partyMembers().getFirst().playerId();

        Optional<Party> found = partyRepository.findByPlayerId(participantUserId);

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaPartyRepository, times(1)).findByPlayerId(participantUserId);
    }

    @Test
    void findByUserId_givenPlayerIsNotOwnerOrPartyMember_shouldReturnEmpty() {
        Optional<Party> found = partyRepository.findByPlayerId(UUID.randomUUID());

        Assertions.assertThat(found).isEmpty();

        verify(jpaPartyRepository, times(1)).findByPlayerId(any());
    }
}
