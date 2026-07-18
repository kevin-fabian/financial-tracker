package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.InvitationNotFoundException;
import com.fabiankevin.app.exceptions.party.NotPartyLeaderException;
import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.PartyService;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
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

import static com.fabiankevin.app.models.enums.party.InvitationStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(InvitationController.class)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartyService partyService;

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

    private InvitationSummary invitationSummary(UUID invitationId, UUID partyId, InvitationStatus status, boolean inviter) {
        return InvitationSummary.builder()
            .id(invitationId)
            .inviterName("John Doe")
            .inviterInitial("JD")
            .inviteeName("Jane Smith")
            .inviteeInitial("JS")
            .proposedSharingModeName(SharingMode.EVEN_SHARE.getName())
            .proposedSharingModeDescription(SharingMode.EVEN_SHARE.getDescription())
            .proposedRoleName(AccessLevel.READ_WRITE.getName())
            .proposedRoleDescription(AccessLevel.READ_WRITE.getDescription())
            .status(status)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86_400))
            .partyId(partyId)
            .partyName("Family 2026 Budget")
            .inviter(inviter)
            .build();
    }

    @Nested
    class GetInvitations {

        @Test
        void givenUserWithInvitations_thenReturnsList() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID sentId = UUID.randomUUID();
            UUID receivedId = UUID.randomUUID();
            InvitationSummary sent = InvitationSummary.builder()
                .id(sentId)
                .inviterName("John Doe")
                .inviterInitial("JD")
                .inviteeName("Jane Smith")
                .inviteeInitial("JS")
                .proposedSharingModeName(SharingMode.EVEN_SHARE.getName())
                .proposedSharingModeDescription(SharingMode.EVEN_SHARE.getDescription())
                .proposedRoleName(AccessLevel.READ_WRITE.getName())
                .proposedRoleDescription(AccessLevel.READ_WRITE.getDescription())
                .status(PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86_400))
                .partyId(partyId)
                .partyName("Family 2026 Budget")
                .inviter(true)
                .build();
            InvitationSummary received = InvitationSummary.builder()
                .id(receivedId)
                .inviterName("Bob Jones")
                .inviterInitial("BJ")
                .inviteeName("Alice Brown")
                .inviteeInitial("AB")
                .proposedSharingModeName(SharingMode.EVEN_SHARE.getName())
                .proposedSharingModeDescription(SharingMode.EVEN_SHARE.getDescription())
                .proposedRoleName(AccessLevel.VIEW_ONLY.getName())
                .proposedRoleDescription(AccessLevel.VIEW_ONLY.getDescription())
                .status(PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86_400))
                .partyId(partyId)
                .partyName("Trip Expenses")
                .inviter(false)
                .build();

            when(invitationService.getInvitationsByUserId(userId)).thenReturn(List.of(sent, received));

            mockMvc.perform(get("/api/parties/invitations")
                .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(sentId.toString()))
                .andExpect(jsonPath("$[0].inviterName").value("John Doe"))
                .andExpect(jsonPath("$[0].inviterInitial").value("JD"))
                .andExpect(jsonPath("$[0].inviteeName").value("Jane Smith"))
                .andExpect(jsonPath("$[0].inviteeInitial").value("JS"))
                .andExpect(jsonPath("$[0].proposedSharingModeName").value("Even Share"))
                .andExpect(jsonPath("$[0].proposedSharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$[0].proposedRoleName").value("Read & Write"))
                .andExpect(jsonPath("$[0].proposedRoleDescription").value(AccessLevel.READ_WRITE.getDescription()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].expiresAt").exists())
                .andExpect(jsonPath("$[0].partyId").value(partyId.toString()))
                .andExpect(jsonPath("$[0].partyName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$[0].inviter").value(true))
                .andExpect(jsonPath("$[1].id").value(receivedId.toString()))
                .andExpect(jsonPath("$[1].inviterName").value("Bob Jones"))
                .andExpect(jsonPath("$[1].inviterInitial").value("BJ"))
                .andExpect(jsonPath("$[1].inviteeName").value("Alice Brown"))
                .andExpect(jsonPath("$[1].inviteeInitial").value("AB"))
                .andExpect(jsonPath("$[1].proposedSharingModeName").value("Even Share"))
                .andExpect(jsonPath("$[1].proposedSharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$[1].proposedRoleName").value("View Only"))
                .andExpect(jsonPath("$[1].proposedRoleDescription").value(AccessLevel.VIEW_ONLY.getDescription()))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].createdAt").exists())
                .andExpect(jsonPath("$[1].expiresAt").exists())
                .andExpect(jsonPath("$[1].partyId").value(partyId.toString()))
                .andExpect(jsonPath("$[1].partyName").value("Trip Expenses"))
                .andExpect(jsonPath("$[1].inviter").value(false));

            verify(invitationService).getInvitationsByUserId(userId);
        }

        @Test
        void givenMissingJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/parties/invitations"))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenUserWithNoInvitations_thenReturnsEmptyList() throws Exception {
            when(invitationService.getInvitationsByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/parties/invitations")
                .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class SendInvitation {

        @Test
        void givenUnrecognizedParameter_thenReturnsBadRequest() throws Exception {
            UUID partyId = UUID.randomUUID();

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations")
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
        void givenValidRequest_thenReturnsInvitation() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            when(invitationService.sendInvitation(any(SendInvitationCommand.class)))
                .thenReturn(invitationSummary(invitationId, partyId, PENDING, true));

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.inviterName").value("John Doe"))
                .andExpect(jsonPath("$.inviterInitial").value("JD"))
                .andExpect(jsonPath("$.inviteeName").value("Jane Smith"))
                .andExpect(jsonPath("$.inviteeInitial").value("JS"))
                .andExpect(jsonPath("$.proposedSharingModeName").value("Even Share"))
                .andExpect(jsonPath("$.proposedSharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$.proposedRoleName").value("Read & Write"))
                .andExpect(jsonPath("$.proposedRoleDescription").value(AccessLevel.READ_WRITE.getDescription()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.partyId").value(partyId.toString()))
                .andExpect(jsonPath("$.partyName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$.inviter").value(true));

            verify(invitationService).sendInvitation(any(SendInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations")
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenNotOwner_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            SendInvitationRequest request = SendInvitationRequest.builder()
                .email("jane@example.com")
                .build();

            when(invitationService.sendInvitation(any())).thenThrow(new NotPartyLeaderException());

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class AcceptInvitation {

        @Test
        void givenPending_thenReturnsInvitation() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            when(invitationService.acceptInvitation(any(AcceptInvitationCommand.class)))
                .thenReturn(invitationSummary(invitationId, partyId, ACCEPTED, false));

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/accept")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.inviterName").value("John Doe"))
                .andExpect(jsonPath("$.inviterInitial").value("JD"))
                .andExpect(jsonPath("$.inviteeName").value("Jane Smith"))
                .andExpect(jsonPath("$.inviteeInitial").value("JS"))
                .andExpect(jsonPath("$.proposedSharingModeName").value("Even Share"))
                .andExpect(jsonPath("$.proposedSharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$.proposedRoleName").value("Read & Write"))
                .andExpect(jsonPath("$.proposedRoleDescription").value(AccessLevel.READ_WRITE.getDescription()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.partyId").value(partyId.toString()))
                .andExpect(jsonPath("$.partyName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$.inviter").value(false));

            verify(invitationService).acceptInvitation(any(AcceptInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/accept"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenNotFound_thenReturnsNotFound() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            doThrow(new InvitationNotFoundException())
                .when(invitationService).acceptInvitation(any());

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/accept")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RejectInvitation {

        @Test
        void givenInvitee_thenReturnsInvitation() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            when(invitationService.rejectInvitation(any(RejectInvitationCommand.class)))
                .thenReturn(invitationSummary(invitationId, partyId, REJECTED, false));

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/reject")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.inviterName").value("John Doe"))
                .andExpect(jsonPath("$.inviterInitial").value("JD"))
                .andExpect(jsonPath("$.inviteeName").value("Jane Smith"))
                .andExpect(jsonPath("$.inviteeInitial").value("JS"))
                .andExpect(jsonPath("$.proposedSharingModeName").value("Even Share"))
                .andExpect(jsonPath("$.proposedSharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$.proposedRoleName").value("Read & Write"))
                .andExpect(jsonPath("$.proposedRoleDescription").value(AccessLevel.READ_WRITE.getDescription()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.partyId").value(partyId.toString()))
                .andExpect(jsonPath("$.partyName").value("Family 2026 Budget"))
                .andExpect(jsonPath("$.inviter").value(false));

            verify(invitationService).rejectInvitation(any(RejectInvitationCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/reject"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenNotInvitee_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID invitationId = UUID.randomUUID();

            when(invitationService.rejectInvitation(any())).thenThrow(new ForbiddenException("Only the invited user can reject"));

            mockMvc.perform(post("/api/parties/" + partyId + "/invitations/" + invitationId + "/reject")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isForbidden());
        }
    }
}
