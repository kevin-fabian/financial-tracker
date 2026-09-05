package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.helper.HouseholdServiceTestHelper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvitationControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @MockitoBean
    private UserClient userClient;

    @Autowired
    private HouseholdService householdService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private HouseholdServiceTestHelper helper;

    @Nested
    class SendInvitation {

        @Test
        void givenValidEmailAndHousehold_thenShouldReturnInvitationResponse() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Mock user lookup for invitee (called by sendInvitation)
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build());

            // Mock user lookup for both inviter and invitee (called by toSummary after save)
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 2)))
                    .thenReturn(List.of(
                            User.builder().id(leaderId).firstName("Alice").lastName("Smith").build(),
                            User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build()
                    ));

            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email(inviteeEmail)
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.isInviter").value(true))
                    .andExpect(jsonPath("$.inviter.id").value(leaderId.toString()))
                    .andExpect(jsonPath("$.inviter.firstName").value("Alice"))
                    .andExpect(jsonPath("$.inviter.lastName").value("Smith"))
                    .andExpect(jsonPath("$.invitee.id").value(inviteeId.toString()))
                    .andExpect(jsonPath("$.invitee.firstName").value("Jane"))
                    .andExpect(jsonPath("$.invitee.lastName").value("Doe"))
                    .andExpect(jsonPath("$.household.id").value(householdSummary.id().toString()))
                    .andExpect(jsonPath("$.household.name").value(householdName))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.expiresAt").exists());
        }

        @Test
        void givenInviteeAlreadyMember_thenShouldReturnBadRequest() throws Exception {
            UUID leaderId = UUID.randomUUID();
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Mock user lookup for leader's email (they're already a member)
            when(userClient.getUserByEmail("alice@example.com"))
                    .thenReturn(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build());

            // Try to invite the leader themselves (already a member)
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email("alice@example.com")
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenInviteeInAnotherHousehold_thenShouldReturnConflict() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Other user creates their own household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.get(0).equals(otherUserId))))
                    .thenReturn(List.of(User.builder().id(otherUserId).firstName("Bob").lastName("Jones").build()));

            householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(otherUserId)
                            .householdName("Bob's Household")
                            .build());

            // Mock user lookup for Bob's email (already in another household)
            when(userClient.getUserByEmail("bob@example.com"))
                    .thenReturn(User.builder().id(otherUserId).firstName("Bob").lastName("Jones").build());

            // Try to invite the other user (already in another household)
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email("bob@example.com")
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenInvalidEmail_thenShouldReturnBadRequest() throws Exception {
            UUID leaderId = UUID.randomUUID();
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Try to send invitation with invalid email
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email("not-an-email")
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNullEmail_thenShouldReturnBadRequest() throws Exception {
            UUID leaderId = UUID.randomUUID();
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Try to send invitation with null email
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email(null)
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNonExistentHousehold_thenShouldReturnNotFound() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID nonExistentHouseholdId = UUID.randomUUID();

            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email("jane@example.com")
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", nonExistentHouseholdId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenDuplicatePendingInvitation_thenShouldReturnExistingInvitation() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Mock user lookup for invitee
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build());

            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email(inviteeEmail)
                    .build();

            // First invitation
            String firstResponse = mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            UUID firstInvitationId = UUID.fromString(jsonMapper.readTree(firstResponse).get("id").asText());

            // Second invitation to same user for same household
            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(firstInvitationId.toString()));
        }
    }

    @Nested
    class GetInvitations {

        @Test
        void givenUserWithPendingInvitationAsInvitee_thenShouldReturnInvitation() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Mock user lookup for invitee
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build());

            // Mock user lookup for both users (sendInvitation → toSummary)
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 2)))
                    .thenReturn(List.of(
                            User.builder().id(leaderId).firstName("Alice").lastName("Smith").build(),
                            User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build()
                    ));

            // Send invitation
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email(inviteeEmail)
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Invitee GETs invitations — should see the invitation as invitee (isInviter=false)
            mockMvc.perform(get("/api/households/invitations")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", inviteeId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").isNotEmpty())
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[0].isInviter").value(false))
                    .andExpect(jsonPath("$[0].inviter.id").value(leaderId.toString()))
                    .andExpect(jsonPath("$[0].inviter.firstName").value("Alice"))
                    .andExpect(jsonPath("$[0].invitee.id").value(inviteeId.toString()))
                    .andExpect(jsonPath("$[0].invitee.firstName").value("Jane"))
                    .andExpect(jsonPath("$[0].household.id").value(householdSummary.id().toString()))
                    .andExpect(jsonPath("$[0].household.name").value(householdName))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].expiresAt").exists());
        }

        @Test
        void givenUserWithPendingInvitationAsInviter_thenShouldReturnInvitation() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            String householdName = "Family Budget";

            // Leader creates household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.getFirst().equals(leaderId))))
                    .thenReturn(List.of(User.builder().id(leaderId).firstName("Alice").lastName("Smith").build()));

            HouseholdSummary householdSummary = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderId)
                            .householdName(householdName)
                            .build());
            assertNotNull(householdSummary.id());

            // Mock user lookup for invitee
            when(userClient.getUserByEmail(inviteeEmail))
                    .thenReturn(User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build());

            // Mock user lookup for both users (sendInvitation → toSummary)
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 2)))
                    .thenReturn(List.of(
                            User.builder().id(leaderId).firstName("Alice").lastName("Smith").build(),
                            User.builder().id(inviteeId).firstName("Jane").lastName("Doe").build()
                    ));

            // Send invitation
            SendInvitationRequest request = SendInvitationRequest.builder()
                    .email(inviteeEmail)
                    .build();

            mockMvc.perform(post("/api/households/{householdId}/invitations", householdSummary.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Leader GETs invitations — should see the invitation as inviter (isInviter=true)
            mockMvc.perform(get("/api/households/invitations")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].isInviter").value(true))
                    .andExpect(jsonPath("$[0].inviter.id").value(leaderId.toString()))
                    .andExpect(jsonPath("$[0].invitee.id").value(inviteeId.toString()));
        }

        @Test
        void givenUserWithNoInvitations_thenShouldReturnEmptyList() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(get("/api/households/invitations")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class AcceptInvitation {

        @Test
        void givenPendingInvitation_thenInviteeCanAccept() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";

            UUID householdId = helper.createHouseHold(leaderId).id();
            UUID invitationId = helper.sendInvitation(householdId, leaderId, inviteeId, inviteeEmail);

            // Invitee accepts the invitation
            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/accept",
                            householdId, invitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", inviteeId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invitationId.toString()))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.isInviter").value(false))
                    .andExpect(jsonPath("$.invitee.id").value(inviteeId.toString()));
        }

        @Test
        void givenPendingInvitation_thenInviterCannotAccept() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";
            String householdName = "Family Budget";

            UUID householdId = helper.createHouseHold(leaderId).id();
            UUID invitationId = helper.sendInvitation(householdId, leaderId, inviteeId, inviteeEmail);

            // Leader (inviter) tries to accept — should fail
            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/accept",
                            householdId, invitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenNonExistentInvitation_thenShouldReturnNotFound() throws Exception {
            UUID inviteeId = UUID.randomUUID();
            UUID nonExistentInvitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/accept",
                            UUID.randomUUID(), nonExistentInvitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", inviteeId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RejectInvitation {

        @Test
        void givenPendingInvitation_thenInviteeCanReject() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";

            UUID householdId = helper.createHouseHold(leaderId).id();
            UUID invitationId = helper.sendInvitation(householdId, leaderId, inviteeId, inviteeEmail);

            // Invitee rejects the invitation
            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/reject",
                            householdId, invitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", inviteeId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invitationId.toString()))
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.isInviter").value(false));
        }

        @Test
        void givenPendingInvitation_thenInviterCanReject() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();
            String inviteeEmail = "jane@example.com";

            UUID householdId = helper.createHouseHold(leaderId).id();
            UUID invitationId = helper.sendInvitation(householdId, leaderId, inviteeId, inviteeEmail);

            // Leader (inviter) rejects the invitation
            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/reject",
                            householdId, invitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invitationId.toString()))
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.isInviter").value(true));
        }

        @Test
        void givenNonExistentInvitation_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID nonExistentInvitationId = UUID.randomUUID();

            mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/reject",
                            UUID.randomUUID(), nonExistentInvitationId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }
    }
}
