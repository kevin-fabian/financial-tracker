package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.ForbiddenException;
import com.fabiankevin.app.exceptions.shared_space.InvitationNotFoundException;
import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.models.enums.shared_space.*;
import com.fabiankevin.app.models.shared_space.*;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.SharedSpaceService;
import com.fabiankevin.app.services.commands.shared_space.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import com.fabiankevin.app.services.commands.shared_space.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.dtos.shared_space.CreateSharedSpaceRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.shared_space.AccessLevel.READ_WRITE;
import static com.fabiankevin.app.models.enums.shared_space.InvitationStatus.PENDING;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(SharedSpaceController.class)
class SharedSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SharedSpaceService sharedSpaceService;

    @MockitoBean
    private InvitationService invitationService;

    @Autowired
    private JsonMapper jsonMapper;

    private Jwt jwt;
    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
            .subject(userId.toString())
            .header("alg", "RS256")
            .audience(List.of("financial-tracker-test"))
            .claim("role", "USER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private SharedSpace spaceWithId(UUID id, UUID ownerId) {
        return SharedSpace.builder()
            .id(id)
            .spaceName("Family 2026 Budget")
            .ownerUserId(ownerId)
            .sharingMode(SharingMode.MUTUAL_SHARING)
            .participants(List.of())
            .sharedResources(List.of())
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private SharedSpaceSummary spaceSummaryWithId(UUID id, UUID ownerId) {
        return SharedSpaceSummary.builder()
            .id(id)
            .spaceName("Family 2026 Budget")
            .ownerUserId(ownerId)
            .sharingMode(SharingMode.MUTUAL_SHARING)
            .participants(List.of())
            .sharedResources(List.of())
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private InvitationSummary invitationSummaryWithId(UUID id, UUID spaceId, UUID inviterId, UUID inviteeUserId, InvitationStatus status) {
        return InvitationSummary.builder()
            .id(id)
            .inviterName("Inviter")
            .inviterInitial("IN")
            .inviteeName("Invitee")
            .inviteeInitial("IV")
            .proposedSharingModeName(SharingMode.MUTUAL_SHARING.getName())
            .proposedSharingModeDescription(SharingMode.MUTUAL_SHARING.getDescription())
            .proposedRoleName(READ_WRITE.getName())
            .proposedRoleDescription(READ_WRITE.getDescription())
            .status(status)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86_400))
            .sharedSpaceId(spaceId)
            .sharedSpaceName("Family 2026 Budget")
            .build();
    }

    private SpaceParticipantSummary participantSummary(UUID id, UUID userId) {
        return SpaceParticipantSummary.builder()
            .id(id)
            .name("John Doe")
            .initial("JD")
            .accessLevel(AccessLevel.READ_WRITE)
            .status(ParticipantStatus.ACTIVE)
            .joinedAt(Instant.now())
            .build();
    }

    private SharedResource sharedResource(UUID id, ResourceType type) {
        return SharedResource.builder()
            .id(id)
            .type(type)
            .items(List.of("item-1", "item-2"))
            .sharedAt(Instant.now())
            .build();
    }

    @Nested
    class CreateSharedSpace {

        @Test
        void givenValidRequest_thenReturnsCreated() throws Exception {
            UUID spaceId = UUID.randomUUID();
            CreateSharedSpaceRequest request = CreateSharedSpaceRequest.builder()
                .spaceName("Family 2026 Budget")
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .build();

            when(sharedSpaceService.createShare(any())).thenReturn(spaceWithId(spaceId, userId));

            mockMvc.perform(post("/api/shared-spaces")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/shared-spaces/[-a-f0-9]{36}")))
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.spaceName").value("Family 2026 Budget"));

            verify(sharedSpaceService).createShare(any(CreateSharedSpaceCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            CreateSharedSpaceRequest request = CreateSharedSpaceRequest.builder()
                .spaceName("Family 2026 Budget")
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .build();

            mockMvc.perform(post("/api/shared-spaces")
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenMissingSharingMode_thenReturnsBadRequest() throws Exception {
            CreateSharedSpaceRequest request = CreateSharedSpaceRequest.builder()
                .spaceName("Family 2026 Budget")
                .build();

            mockMvc.perform(post("/api/shared-spaces")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(sharedSpaceService);
        }
    }

    @Nested
    class GetSharedSpaces {

        @Test
        void givenUserWithSpaces_thenReturnsList() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            SharedSpaceSummary space = SharedSpaceSummary.builder()
                .id(spaceId)
                .spaceName("Family 2026 Budget")
                .ownerUserId(userId)
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .participants(List.of(participantSummary(participantId, userId)))
                .sharedResources(List.of(sharedResource(resourceId, ResourceType.TRANSACTION)))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of(space));

            mockMvc.perform(get("/api/shared-spaces")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(spaceId.toString()))
                .andExpect(jsonPath("$[0].spaceName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$[0].ownerUserId").value(userId.toString()))
                .andExpect(jsonPath("$[0].sharingModeName").value("Mutual Sharing"))
                .andExpect(jsonPath("$[0].sharingModeDescription").value(SharingMode.MUTUAL_SHARING.getDescription()))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].participants.length()").value(1))
                .andExpect(jsonPath("$[0].participants[0].id").value(participantId.toString()))
                .andExpect(jsonPath("$[0].participants[0].userId").value(participantId.toString()))
                .andExpect(jsonPath("$[0].participants[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].participants[0].initial").value("JD"))
                .andExpect(jsonPath("$[0].participants[0].accessLevelName").value("Read & Write"))
                .andExpect(jsonPath("$[0].participants[0].accessLevelDescription").value(READ_WRITE.getDescription()))
                .andExpect(jsonPath("$[0].participants[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].participants[0].joinedAt").exists())
                .andExpect(jsonPath("$[0].sharedResources.length()").value(1))
                .andExpect(jsonPath("$[0].sharedResources[0].id").value(resourceId.toString()))
                .andExpect(jsonPath("$[0].sharedResources[0].type").value("TRANSACTION"))
                .andExpect(jsonPath("$[0].sharedResources[0].name").value("Transaction"))
                .andExpect(jsonPath("$[0].sharedResources[0].description").value(ResourceType.TRANSACTION.getDescription()))
                .andExpect(jsonPath("$[0].sharedResources[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].sharedResources[0].items[0]").value("item-1"))
                .andExpect(jsonPath("$[0].sharedResources[0].sharedAt").exists());

            verify(sharedSpaceService).retrieveByUserId(userId);
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/shared-spaces"))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenUserWithNoSpaces_thenReturnsEmptyList() throws Exception {
            when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/shared-spaces")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class GetInvitations {

        @Test
        void givenUserWithInvitations_thenReturnsList() throws Exception {
            UUID inviterId = UUID.randomUUID();
            UUID spaceId = UUID.randomUUID();
            UUID sentId = UUID.randomUUID();
            UUID receivedId = UUID.randomUUID();
            InvitationSummary sent = InvitationSummary.builder()
                .id(sentId)
                .inviterName("John Doe")
                .inviterInitial("JD")
                .inviteeName("Jane Smith")
                .inviteeInitial("JS")
                .proposedSharingModeName(SharingMode.MUTUAL_SHARING.getName())
                .proposedSharingModeDescription(SharingMode.MUTUAL_SHARING.getDescription())
                .proposedRoleName(AccessLevel.READ_WRITE.getName())
                .proposedRoleDescription(AccessLevel.READ_WRITE.getDescription())
                .status(PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86_400))
                .sharedSpaceId(spaceId)
                .sharedSpaceName("Family 2026 Budget")
                .build();
            InvitationSummary received = InvitationSummary.builder()
                .id(receivedId)
                .inviterName("Bob Jones")
                .inviterInitial("BJ")
                .inviteeName("Alice Brown")
                .inviteeInitial("AB")
                .proposedSharingModeName(SharingMode.MUTUAL_SHARING.getName())
                .proposedSharingModeDescription(SharingMode.MUTUAL_SHARING.getDescription())
                .proposedRoleName(AccessLevel.VIEW_ONLY.getName())
                .proposedRoleDescription(AccessLevel.VIEW_ONLY.getDescription())
                .status(PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86_400))
                .sharedSpaceId(spaceId)
                .sharedSpaceName("Trip Expenses")
                .build();

            when(invitationService.getInvitationsByUserId(userId)).thenReturn(List.of(sent, received));

            mockMvc.perform(get("/api/shared-spaces/invitations")
                .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(sentId.toString()))
                .andExpect(jsonPath("$[0].inviterName").value("John Doe"))
                .andExpect(jsonPath("$[0].inviterInitial").value("JD"))
                .andExpect(jsonPath("$[0].inviteeName").value("Jane Smith"))
                .andExpect(jsonPath("$[0].inviteeInitial").value("JS"))
                .andExpect(jsonPath("$[0].proposedSharingModeName").value("Mutual Sharing"))
                .andExpect(jsonPath("$[0].proposedSharingModeDescription").value(SharingMode.MUTUAL_SHARING.getDescription()))
                .andExpect(jsonPath("$[0].proposedRoleName").value("Read & Write"))
                .andExpect(jsonPath("$[0].proposedRoleDescription").value(AccessLevel.READ_WRITE.getDescription()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].expiresAt").exists())
                .andExpect(jsonPath("$[0].sharedSpaceId").value(spaceId.toString()))
                .andExpect(jsonPath("$[0].sharedSpaceName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$[1].id").value(receivedId.toString()))
                .andExpect(jsonPath("$[1].inviterName").value("Bob Jones"))
                .andExpect(jsonPath("$[1].inviterInitial").value("BJ"))
                .andExpect(jsonPath("$[1].inviteeName").value("Alice Brown"))
                .andExpect(jsonPath("$[1].inviteeInitial").value("AB"))
                .andExpect(jsonPath("$[1].proposedSharingModeName").value("Mutual Sharing"))
                .andExpect(jsonPath("$[1].proposedSharingModeDescription").value(SharingMode.MUTUAL_SHARING.getDescription()))
                .andExpect(jsonPath("$[1].proposedRoleName").value("View Only"))
                .andExpect(jsonPath("$[1].proposedRoleDescription").value(AccessLevel.VIEW_ONLY.getDescription()))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].expiresAt").exists())
                .andExpect(jsonPath("$[1].sharedSpaceId").value(spaceId.toString()))
                .andExpect(jsonPath("$[1].sharedSpaceName").value("Trip Expenses"));

            verify(invitationService).getInvitationsByUserId(userId);
        }

        @Test
        void givenMissingJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/shared-spaces/invitations"))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenUserWithNoInvitations_thenReturnsEmptyList() throws Exception {
            when(invitationService.getInvitationsByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/shared-spaces/invitations")
                .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class SendInvitation {

        @Test
        void givenUnrecognizedParameter_thenReturnsBadRequest() throws Exception {
            UUID spaceId = UUID.randomUUID();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content("""
                                    {
                                     "invalid_field": "jane@example.com"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(invitationService);
        }

        @Test
        void givenValidRequest_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

            verify(invitationService).sendInvitation(any(SendInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenNotOwner_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            when(invitationService.sendInvitation(any())).thenThrow(new NotSpaceOwnerException());

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class AcceptInvitation {

        @Test
        void givenPending_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

            verify(invitationService).acceptInvitation(any(AcceptInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenNotFound_thenReturnsNotFound() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            doThrow(new InvitationNotFoundException())
                .when(invitationService).acceptInvitation(any());

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RejectInvitation {

        @Test
        void givenInvitee_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

            verify(invitationService).rejectInvitation(any(RejectInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenNotInvitee_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            when(invitationService.rejectInvitation(any())).thenThrow(new ForbiddenException("Only the invited user can reject"));

            mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class RemoveParticipant {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

            verify(sharedSpaceService).removeParticipant(spaceId, participantId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId))
                .andExpect(status().isForbidden());

            verifyNoInteractions(sharedSpaceService);
        }

        @Test
        void givenCannotRemoveOwner_thenReturnsConflict() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID ownerParticipantId = UUID.randomUUID();

            doThrow(new CannotRemoveOwnerException())
                .when(sharedSpaceService).removeParticipant(spaceId, ownerParticipantId, userId);

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + ownerParticipantId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isConflict());
        }
    }
}
