package com.fabiankevin.app.web.controllers.helper;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.dtos.party.HouseholdResponse;
import com.fabiankevin.app.web.controllers.dtos.party.InvitationResponse;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@Component
public class HouseholdServiceTestHelper {
    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final UserClient userClient;

    public HouseholdResponse createHouseHold(UUID userId) throws Exception {
        OrganizeHouseholdRequest request =  OrganizeHouseholdRequest.builder()
                .name("Test Household")
                .build();

        MvcResult mvcResult = mockMvc.perform(post("/api/households")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("USER"))
                                .jwt(jwt -> jwt
                                        .audience(List.of("financial-tracker-test"))
                                        .claim("sub", userId)
                                        .claim("scope", List.of())
                                ))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        mvcResult.getResponse().getContentAsString();
        return jsonMapper.readValue(mvcResult.getResponse().getContentAsString(), HouseholdResponse.class);
    }

    public InvitationResponse inviteAndAccept(UUID partyId, UUID partyLeaderId, UUID inviteeId, String inviteeEmail) throws Exception {
        // Step 1: Party leader sends invitation
        SendInvitationRequest sendRequest = SendInvitationRequest.builder()
                .email(inviteeEmail)
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/households/{householdId}/invitations", partyId)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("USER"))
                                .jwt(jwt -> jwt
                                        .audience(List.of("financial-tracker-test"))
                                        .claim("sub", partyLeaderId)
                                        .claim("scope", List.of())))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andReturn();

        InvitationResponse invitation = jsonMapper.readValue(
                sendResult.getResponse().getContentAsString(), InvitationResponse.class);

        // Step 2: Invitee accepts the invitation
        MvcResult acceptResult = mockMvc.perform(post("/api/households/{householdId}/invitations/{invitationId}/accept",
                        partyId, invitation.id())
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("USER"))
                                .jwt(jwt -> jwt
                                        .audience(List.of("financial-tracker-test"))
                                        .claim("sub", inviteeId)
                                        .claim("scope", List.of())))
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();

        return jsonMapper.readValue(
                acceptResult.getResponse().getContentAsString(), InvitationResponse.class);
    }

    public UUID sendInvitation(UUID householdId, UUID leaderId, UUID inviteeId, String inviteeEmail) throws Exception {
        // Set up userClient mocks needed by the invitation service flow
        when(userClient.getUserByEmail(inviteeEmail))
                .thenReturn(User.builder().id(inviteeId).firstName("Invitee").lastName("User").build());
        when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 2)))
                .thenReturn(List.of(
                        User.builder().id(leaderId).firstName("Leader").lastName("User").build(),
                        User.builder().id(inviteeId).firstName("Invitee").lastName("User").build()
                ));

        SendInvitationRequest sendRequest = SendInvitationRequest.builder()
                .email(inviteeEmail)
                .build();

        MvcResult result = mockMvc.perform(post("/api/households/{householdId}/invitations", householdId)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("USER"))
                                .jwt(jwt -> jwt
                                        .audience(List.of("financial-tracker-test"))
                                        .claim("sub", leaderId)
                                        .claim("scope", List.of())))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(
                jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
