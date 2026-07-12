package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.persistence.jpa_repositories.JpaInvitationRepository;
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
                .inviteeEmail("invitee@example.com")
                .inviteeUserId(null)
                .proposedSharingMode(SharingMode.MUTUAL_SHARING)
                .proposedRole(AccessLevel.READ_WRITE)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .sharedSpaceId(null)
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
        Assertions.assertThat(restored.inviteeEmail()).isEqualTo("invitee@example.com");
        Assertions.assertThat(restored.inviteeUserId()).isNull();
        Assertions.assertThat(restored.proposedSharingMode()).isEqualTo(SharingMode.MUTUAL_SHARING);
        Assertions.assertThat(restored.proposedRole()).isEqualTo(AccessLevel.READ_WRITE);
        Assertions.assertThat(restored.status()).isEqualTo(InvitationStatus.PENDING);
        Assertions.assertThat(restored.createdAt()).isEqualTo(invitation.createdAt());
        Assertions.assertThat(restored.expiresAt()).isEqualTo(invitation.expiresAt());
        Assertions.assertThat(restored.sharedSpaceId()).isNull();

        verify(jpaInvitationRepository, times(1)).save(any());
        verify(jpaInvitationRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_givenAcceptedInvitationWithResultingSpace_shouldPersistAllFields() {
        UUID resultingSpaceId = UUID.randomUUID();
        UUID inviteeUserId = UUID.randomUUID();

        Invitation accepted = Invitation.builder()
                .inviterUserId(UUID.randomUUID())
                .inviteeEmail("accepted@example.com")
                .inviteeUserId(inviteeUserId)
                .proposedSharingMode(SharingMode.MUTUAL_SHARING)
                .proposedRole(AccessLevel.READ_WRITE)
                .status(InvitationStatus.ACCEPTED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .sharedSpaceId(resultingSpaceId)
                .build();

        Invitation saved = invitationRepository.save(accepted);

        Optional<Invitation> found = invitationRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().status()).isEqualTo(InvitationStatus.ACCEPTED);
        Assertions.assertThat(found.get().inviteeUserId()).isEqualTo(inviteeUserId);
        Assertions.assertThat(found.get().sharedSpaceId()).isEqualTo(resultingSpaceId);
    }

    @Test
    void findById_givenExistingInvitation_shouldReturnInvitation() {
        Invitation saved = invitationRepository.save(invitation);

        Optional<Invitation> found = invitationRepository.findById(saved.id());

        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().inviteeEmail()).isEqualTo("invitee@example.com");
        Assertions.assertThat(found.get().status()).isEqualTo(InvitationStatus.PENDING);
        Assertions.assertThat(found.get().proposedSharingMode()).isEqualTo(SharingMode.MUTUAL_SHARING);
        Assertions.assertThat(found.get().proposedRole()).isEqualTo(AccessLevel.READ_WRITE);

        verify(jpaInvitationRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        Optional<Invitation> found = invitationRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }
}
