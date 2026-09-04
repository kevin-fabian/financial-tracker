package com.fabiankevin.app.web.controllers.helper;

import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizePartyRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PartyResponse;
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

    public PartyResponse createHouseHold(UUID userId) throws Exception {
        OrganizePartyRequest request =  OrganizePartyRequest.builder()
                .name("Test Party")
                .sharingMode(SharingMode.EVEN_SHARE)
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
        return jsonMapper.readValue(mvcResult.getResponse().getContentAsString(), PartyResponse.class);
    }
}
