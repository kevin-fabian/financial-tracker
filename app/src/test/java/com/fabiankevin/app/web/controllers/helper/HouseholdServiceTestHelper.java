package com.fabiankevin.app.web.controllers.helper;

import com.fabiankevin.app.models.household.InvitationSummary;
import com.fabiankevin.app.web.controllers.dtos.SendInvitationRequest;
import com.fabiankevin.app.web.controllers.dtos.party.HouseholdResponse;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@Component
public class HouseholdServiceTestHelper {
    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public HouseholdResponse createHouseHold(UUID userId) throws Exception {
        OrganizeHouseholdRequest request =  OrganizeHouseholdRequest.builder()
                .name("Test Household")
                .build();

        MvcResult mvcResult = mockMvc.perform(post("/api/parties")
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

    public InvitationSummary inviteAndAccept(UUID partyId, UUID partyLeaderId, UUID inviteeId, String inviteeEmail) throws Exception {
        // Step 1: Party leader sends invitation
        SendInvitationRequest sendRequest = SendInvitationRequest.builder()
                .email(inviteeEmail)
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/parties/{partyId}/invitations", partyId)
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

        InvitationSummary invitation = jsonMapper.readValue(
                sendResult.getResponse().getContentAsString(), InvitationSummary.class);

        // Step 2: Invitee accepts the invitation
        MvcResult acceptResult = mockMvc.perform(post("/api/parties/{partyId}/invitations/{invitationId}/accept",
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
                acceptResult.getResponse().getContentAsString(), InvitationSummary.class);
    }
}
