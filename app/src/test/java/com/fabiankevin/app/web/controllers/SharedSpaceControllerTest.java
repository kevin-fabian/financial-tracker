package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.ForbiddenException;
import com.fabiankevin.app.exceptions.shared_space.InvitationNotFoundException;
import com.fabiankevin.app.exceptions.shared_space.NotSpaceOwnerException;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.services.SharedSpaceService;
import com.fabiankevin.app.services.commands.shared_space.*;
import com.fabiankevin.app.web.controllers.dtos.CreateSharedSpaceRequest;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(SharedSpaceController.class)
class SharedSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SharedSpaceService sharedSpaceService;

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

    private Invitation invitationWithId(UUID id, UUID spaceId, UUID inviterId, UUID inviteeUserId, InvitationStatus status) {
        return Invitation.builder()
            .id(id)
            .inviterUserId(inviterId)
            .inviteeUserId(inviteeUserId)
            .proposedSharingMode(SharingMode.MUTUAL_SHARING)
            .proposedRole(READ_WRITE)
            .status(status)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86_400))
            .sharedSpaceId(spaceId)
            .build();
    }

    // ---- createSharedSpace ----

    @Test
    void createSharedSpace_givenValidRequest_thenReturnsCreated() throws Exception {
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
    void createSharedSpace_givenMissingJwt_thenReturnsForbidden() throws Exception {
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
    void createSharedSpace_givenMissingSharingMode_thenReturnsBadRequest() throws Exception {
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

    // ---- getSharedSpaces ----

    @Test
    void getSharedSpaces_givenUserWithSpaces_thenReturnsList() throws Exception {
        UUID spaceId = UUID.randomUUID();
        when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of(spaceWithId(spaceId, userId)));

        mockMvc.perform(get("/api/shared-spaces")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(spaceId.toString()));

        verify(sharedSpaceService).retrieveByUserId(userId);
    }

    @Test
    void getSharedSpaces_givenNoJwt_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/shared-spaces"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void getSharedSpaces_givenUserWithNoSpaces_thenReturnsEmptyList() throws Exception {
        when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/shared-spaces")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- sendInvitation ----

    @Test
    void sendInvitation_givenOwner_thenReturnsCreated() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        UUID inviteeUserId = UUID.randomUUID();
        SendInvitationRequest request = SendInvitationRequest.builder()
            .inviteeUserId(inviteeUserId)
            .proposedRole(READ_WRITE)
            .build();

        when(sharedSpaceService.sendInvitation(any())).thenReturn(
            invitationWithId(invitationId, spaceId, userId, inviteeUserId, PENDING));

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                .with(jwt().jwt(jwt))
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(invitationId.toString()));

        verify(sharedSpaceService).sendInvitation(any(SendInvitationCommand.class));
    }

    @Test
    void sendInvitation_givenMissingJwt_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        SendInvitationRequest request = SendInvitationRequest.builder()
            .inviteeUserId(UUID.randomUUID())
            .proposedRole(READ_WRITE)
            .build();

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void sendInvitation_givenNotOwner_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        SendInvitationRequest request = SendInvitationRequest.builder()
            .inviteeUserId(UUID.randomUUID())
            .proposedRole(READ_WRITE)
            .build();

        when(sharedSpaceService.sendInvitation(any())).thenThrow(new NotSpaceOwnerException());

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations")
                .with(jwt().jwt(jwt))
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ---- acceptInvitation ----

    @Test
    void acceptInvitation_givenPending_thenReturnsUpdatedSpace() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(sharedSpaceService.acceptInvitation(any())).thenReturn(spaceWithId(spaceId, userId));

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(spaceId.toString()));

        verify(sharedSpaceService).acceptInvitation(any(AcceptInvitationCommand.class));
    }

    @Test
    void acceptInvitation_givenMissingJwt_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void acceptInvitation_givenNotFound_thenReturnsNotFound() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(sharedSpaceService.acceptInvitation(any())).thenThrow(new InvitationNotFoundException());

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/accept")
                .with(jwt().jwt(jwt)))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    // ---- rejectInvitation ----

    @Test
    void rejectInvitation_givenInvitee_thenReturnsRejectedInvitation() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        Invitation rejected = invitationWithId(invitationId, spaceId, UUID.randomUUID(), userId, InvitationStatus.REJECTED);
        when(sharedSpaceService.rejectInvitation(any())).thenReturn(rejected);

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId.toString()))
            .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(sharedSpaceService).rejectInvitation(any(RejectInvitationCommand.class));
    }

    @Test
    void rejectInvitation_givenMissingJwt_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void rejectInvitation_givenNotInvitee_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(sharedSpaceService.rejectInvitation(any())).thenThrow(new ForbiddenException("Only the invited user can reject"));

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/reject")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isForbidden());
    }

    // ---- revokeInvitation ----

    @Test
    void revokeInvitation_givenInviter_thenReturnsRevokedInvitation() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        Invitation revoked = invitationWithId(invitationId, spaceId, UUID.randomUUID(), UUID.randomUUID(), InvitationStatus.REVOKED);
        when(sharedSpaceService.revokeInvitation(any())).thenReturn(revoked);

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/revoke")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId.toString()))
            .andExpect(jsonPath("$.status").value("REVOKED"));

        verify(sharedSpaceService).revokeInvitation(any(RevokeInvitationCommand.class));
    }

    @Test
    void revokeInvitation_givenMissingJwt_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/revoke"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void revokeInvitation_givenNotInviter_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(sharedSpaceService.revokeInvitation(any())).thenThrow(new ForbiddenException("Only the inviter can revoke"));

        mockMvc.perform(post("/api/shared-spaces/" + spaceId + "/invitations/" + invitationId + "/revoke")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isForbidden());
    }

    // ---- removeParticipant ----

    @Test
    void removeParticipant_givenOwner_thenReturnsNoContent() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId)
                .with(jwt().jwt(jwt)))
            .andExpect(status().isNoContent());

        verify(sharedSpaceService).removeParticipant(spaceId, participantId, userId);
    }

    @Test
    void removeParticipant_givenMissingJwt_thenReturnsForbidden() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId))
            .andExpect(status().isForbidden());

        verifyNoInteractions(sharedSpaceService);
    }

    @Test
    void removeParticipant_givenCannotRemoveOwner_thenReturnsConflict() throws Exception {
        UUID spaceId = UUID.randomUUID();
        UUID ownerParticipantId = UUID.randomUUID();

        doThrow(new CannotRemoveOwnerException())
            .when(sharedSpaceService).removeParticipant(spaceId, ownerParticipantId, userId);

        mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + ownerParticipantId)
                .with(jwt().jwt(jwt)))
            .andExpect(status().isConflict());
    }
}
