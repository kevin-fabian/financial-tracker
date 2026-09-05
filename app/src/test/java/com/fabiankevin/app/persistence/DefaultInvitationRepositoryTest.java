package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.Invitation;
import com.fabiankevin.app.persistence.jpa_repositories.JpaInvitationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

@Import(DefaultInvitationRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultInvitationRepositoryTest {

    @MockitoSpyBean
    private JpaInvitationRepository jpaInvitationRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    private Invitation invitation;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public InvitationRepository invitationRepository(JpaInvitationRepository jpaInvitationRepository) {
            return new DefaultInvitationRepository(jpaInvitationRepository);
        }
    }

    @BeforeEach
    void setUp() {
        invitation = Invitation.builder()
                .inviterUserId(UUID.randomUUID())
                .inviteeUserId(UUID.randomUUID())
                .proposedRole(AccessLevel.VIEW_ONLY)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .householdId(null)
                .build();
    }

    @Test
    void save_givenNewInvitation_shouldPersistAndRetrieve() {
        Invitation saved = invitationRepository.save(invitation);

        Optional<Invitation> found = invitationRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Invitation restored = found.get();
        Assertions.assertThat(restored.id()).as("generated id should be present").isNotNull();
        Assertions.assertThat(restored.inviterUserId()).isEqualTo(invitation.inviterUserId());
        Assertions.assertThat(restored.inviteeUserId()).isEqualTo(invitation.inviteeUserId());
        Assertions.assertThat(restored.proposedRole()).isEqualTo(AccessLevel.VIEW_ONLY);
        Assertions.assertThat(restored.status()).isEqualTo(InvitationStatus.PENDING);
        Assertions.assertThat(restored.createdAt()).isEqualTo(invitation.createdAt());
        Assertions.assertThat(restored.expiresAt()).isEqualTo(invitation.expiresAt());
        Assertions.assertThat(restored.householdId()).isNull();

        verify(jpaInvitationRepository, times(1)).save(any());
        verify(jpaInvitationRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenAcceptedInvitationWithPartyId_shouldPersistAllFields() {
        UUID resultingSpaceId = UUID.randomUUID();
        UUID inviteeUserId = UUID.randomUUID();

        Invitation accepted = Invitation.builder()
                .inviterUserId(UUID.randomUUID())
                .inviteeUserId(inviteeUserId)
                .proposedRole(AccessLevel.VIEW_ONLY)
                .status(InvitationStatus.ACCEPTED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .householdId(resultingSpaceId)
                .build();

        Invitation saved = invitationRepository.save(accepted);

        Optional<Invitation> found = invitationRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().status()).isEqualTo(InvitationStatus.ACCEPTED);
        Assertions.assertThat(found.get().inviteeUserId()).isEqualTo(inviteeUserId);
        Assertions.assertThat(found.get().householdId()).isEqualTo(resultingSpaceId);
    }

    @Test
    void findById_givenExistingInvitation_shouldReturnInvitation() {
        Invitation saved = invitationRepository.save(invitation);

        Optional<Invitation> found = invitationRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().inviteeUserId()).isEqualTo(invitation.inviteeUserId());
        Assertions.assertThat(found.get().status()).isEqualTo(InvitationStatus.PENDING);
        Assertions.assertThat(found.get().proposedRole()).isEqualTo(AccessLevel.VIEW_ONLY);

        verify(jpaInvitationRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        Optional<Invitation> found = invitationRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Nested
    class FindByInviteeUserIdTest {

        @Test
        void givenPendingInvitationsWhereUserIsInvitee_shouldReturnIncomingInvites() {
            UUID userId = UUID.randomUUID();
            Invitation incoming = Invitation.builder()
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(userId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(null)
                    .build();
            Invitation outgoing = Invitation.builder()
                    .inviterUserId(userId)
                    .inviteeUserId(UUID.randomUUID())
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(null)
                    .build();
            Invitation savedIncoming = invitationRepository.save(incoming);
            invitationRepository.save(outgoing);

            List<Invitation> found = invitationRepository.findByInviteeUserId(userId);

            Assertions.assertThat(found).hasSize(1);
            Assertions.assertThat(found).extracting(Invitation::id).containsExactly(savedIncoming.id());

            verify(jpaInvitationRepository, times(1)).findByInviteeUserId(
                    userId, InvitationStatus.PENDING, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }

        @Test
        void givenNonPendingInvitations_shouldExcludeFromResults() {
            UUID userId = UUID.randomUUID();
            Invitation accepted = Invitation.builder()
                    .inviterUserId(UUID.randomUUID())
                    .inviteeUserId(userId)
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(UUID.randomUUID())
                    .build();
            invitationRepository.save(accepted);

            List<Invitation> found = invitationRepository.findByInviteeUserId(userId);

            Assertions.assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindPendingByHouseholdIdAndInviterAndInviteeTest {

        @Test
        void givenMatchingPendingInvitation_shouldReturnInvitation() {
            UUID householdId = UUID.randomUUID();
            Invitation pending = Invitation.builder()
                    .inviterUserId(invitation.inviterUserId())
                    .inviteeUserId(invitation.inviteeUserId())
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(householdId)
                    .build();
            Invitation saved = invitationRepository.save(pending);

            Optional<Invitation> found = invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(
                    householdId, saved.inviterUserId(), saved.inviteeUserId());

            Assertions.assertThat(found).isPresent();
            Assertions.assertThat(found.get().id()).isEqualTo(saved.id());
            Assertions.assertThat(found.get().householdId()).isEqualTo(householdId);
            Assertions.assertThat(found.get().status()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        void givenDifferentHousehold_shouldReturnEmpty() {
            UUID householdId = UUID.randomUUID();
            Invitation pending = Invitation.builder()
                    .inviterUserId(invitation.inviterUserId())
                    .inviteeUserId(invitation.inviteeUserId())
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.PENDING)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(householdId)
                    .build();
            Invitation saved = invitationRepository.save(pending);

            Optional<Invitation> found = invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(
                    UUID.randomUUID(), saved.inviterUserId(), saved.inviteeUserId());

            Assertions.assertThat(found).isEmpty();
        }

        @Test
        void givenNonPendingStatus_shouldReturnEmpty() {
            UUID householdId = UUID.randomUUID();
            Invitation accepted = Invitation.builder()
                    .inviterUserId(invitation.inviterUserId())
                    .inviteeUserId(invitation.inviteeUserId())
                    .proposedRole(AccessLevel.VIEW_ONLY)
                    .status(InvitationStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .householdId(householdId)
                    .build();
            Invitation saved = invitationRepository.save(accepted);

            Optional<Invitation> found = invitationRepository.findPendingByHouseholdIdAndInviterAndInvitee(
                    householdId, saved.inviterUserId(), saved.inviteeUserId());

            Assertions.assertThat(found).isEmpty();
        }
    }
}
