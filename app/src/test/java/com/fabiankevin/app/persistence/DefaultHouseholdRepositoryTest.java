package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdMember;
import com.fabiankevin.app.persistence.jpa_repositories.JpaHouseholdRepository;
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

import static com.fabiankevin.app.models.enums.household.AccessLevel.VIEW_ONLY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(DefaultHouseholdRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultHouseholdRepositoryTest {

    @MockitoSpyBean
    private JpaHouseholdRepository jpaHouseholdRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    private Household household;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public HouseholdRepository householdRepository(JpaHouseholdRepository jpaHouseholdRepository) {
            return new DefaultHouseholdRepository(jpaHouseholdRepository);
        }
    }

    @BeforeEach
    void setUp() {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();

        HouseholdMember member = HouseholdMember.builder()
                .userId(memberUserId)
                .accessLevel(VIEW_ONLY)
                .status(HouseholdMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        household = Household.builder()
                .name("Family 2026 Budget")
                .leaderId(ownerUserId)
                .members(List.of(member))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenNewHousehold_shouldPersistAndRetrieveAllFields() {
        Household saved = householdRepository.save(household);

        Optional<Household> optRetrievedHousehold = householdRepository.findById(saved.id());

        Assertions.assertThat(optRetrievedHousehold).isPresent();
        Household retrievedHousehold = optRetrievedHousehold.get();
        Assertions.assertThat(retrievedHousehold.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(retrievedHousehold.name()).isEqualTo("Family 2026 Budget");
        Assertions.assertThat(retrievedHousehold.leaderId()).isEqualTo(household.leaderId());
        Assertions.assertThat(retrievedHousehold.active()).isTrue();
        Assertions.assertThat(retrievedHousehold.createdAt()).isEqualTo(household.createdAt());
        Assertions.assertThat(retrievedHousehold.updatedAt()).isEqualTo(household.updatedAt());

        Assertions.assertThat(retrievedHousehold.members())
                .as("members should be persisted and retrieved")
                .hasSize(1);
        HouseholdMember retrievedMember = retrievedHousehold.members().getFirst();
        Assertions.assertThat(retrievedMember.id()).as("member id should be generated").isNotNull();
        Assertions.assertThat(retrievedMember.userId())
                .isEqualTo(household.members().getFirst().userId());
        Assertions.assertThat(retrievedMember.accessLevel()).isEqualTo(VIEW_ONLY);
        Assertions.assertThat(retrievedMember.status()).isEqualTo(HouseholdMemberStatus.ACTIVE);
        Assertions.assertThat(retrievedMember.joinedAt())
                .isEqualTo(household.members().getFirst().joinedAt());

        verify(jpaHouseholdRepository, times(1)).save(any());
        verify(jpaHouseholdRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenHouseholdWithHouseholdMember_shouldCascadePersist() {
        Household saved = householdRepository.save(household);

        Optional<Household> found = householdRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().members())
                .as("members should be persisted and retrieved")
                .hasSize(1);
        Assertions.assertThat(found.get().members().getFirst().userId())
                .isEqualTo(household.members().getFirst().userId());
        Assertions.assertThat(found.get().members().getFirst().accessLevel())
                .isEqualTo(VIEW_ONLY);
    }

    @Test
    void save_givenHouseholdWithNullLists_shouldPersistWithEmptyLists() {
        Household minimal = Household.builder()
                .name("Empty Space")
                .leaderId(UUID.randomUUID())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Household saved = householdRepository.save(minimal);

        Optional<Household> found = householdRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().members()).isEmpty();
    }

    @Test
    void findById_givenExistingHousehold_shouldReturnExistingHousehold() {
        Household saved = householdRepository.save(household);

        Optional<Household> found = householdRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().name()).isEqualTo("Family 2026 Budget");

        verify(jpaHouseholdRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExistingHousehold_shouldReturnEmptyOptional() {
        Optional<Household> found = householdRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void findByUserId_givenUserIsLeader_shouldReturnHousehold() {
        Household saved = householdRepository.save(household);

        Optional<Household> found = householdRepository.findByUserId(saved.leaderId());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaHouseholdRepository, times(1)).findByUserId(saved.leaderId());
    }

    @Test
    void findByUserId_givenUserIsHouseholdMember_shouldReturnHousehold() {
        Household saved = householdRepository.save(household);
        UUID memberUserId = saved.members().getFirst().userId();

        Optional<Household> found = householdRepository.findByUserId(memberUserId);

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());

        verify(jpaHouseholdRepository, times(1)).findByUserId(memberUserId);
    }

    @Test
    void findByUserId_givenUserIsNotLeaderOrHouseholdMember_shouldReturnEmpty() {
        Optional<Household> found = householdRepository.findByUserId(UUID.randomUUID());

        Assertions.assertThat(found).isEmpty();

        verify(jpaHouseholdRepository, times(1)).findByUserId(any());
    }
}
