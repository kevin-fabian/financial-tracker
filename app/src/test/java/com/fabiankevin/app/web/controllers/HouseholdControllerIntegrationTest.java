package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PatchHouseholdRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HouseholdControllerIntegrationTest {
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
    private JsonMapper jsonMapper;

    @Nested
    class OrganizeHousehold {

        @Test
        void givenValidRequest_thenShouldReturnCreatedAndAllFields() throws Exception {
            UUID userId = UUID.randomUUID();
            String householdName = "Family 2026 Budget";

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name(householdName)
                    .build();

            mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/households/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value(householdName))
                    .andExpect(jsonPath("$.leaderId").value(userId.toString()))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.members").isArray())
                    .andExpect(jsonPath("$.members.length()").value(1))
                    .andExpect(jsonPath("$.members[0].user.id").value(userId.toString()))
                    .andExpect(jsonPath("$.members[0].user.firstName").value("Alice"))
                    .andExpect(jsonPath("$.members[0].user.lastName").value("Smith"))
                    .andExpect(jsonPath("$.members[0].user.initial").value("AS"))
                    .andExpect(jsonPath("$.members[0].householdLeader").value(true))
                    .andExpect(jsonPath("$.members[0].status").value("ACTIVE"));
        }

        @Test
        void givenRequestWithOptionalFields_thenShouldReturnCreatedWithDefaults() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name("Default Household")
                    .build();

            mockMvc.perform(post("/api/households")
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
                    .andExpect(jsonPath("$.name").value("Default Household"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.members.length()").value(1));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void givenRequestWithNullOrEmptyName_thenShouldReturnCreatedWithDefaultName(String name) throws Exception {
            UUID userId = UUID.randomUUID();

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name(name)
                    .build();

            mockMvc.perform(post("/api/households")
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
                    .andExpect(jsonPath("$.name").value("New Household"));
        }

        @Test
        void givenRequestWithTooLongName_thenShouldReturnBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            String longName = "A".repeat(101);

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name(longName)
                    .build();

            mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenUserWithExistingHousehold_thenShouldReturnBadRequest() throws Exception {
            UUID leaderUserId = UUID.randomUUID();

            when(userClient.getUsersByIds(any()))
                    .thenReturn(List.of(
                            User.builder().id(leaderUserId).firstName("Alice").lastName("Smith").build()
                    ));

            HouseholdSummary existingHousehold = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderUserId)
                            .householdName("First Household")
                            .build());
            assertNotNull(existingHousehold.id());

            OrganizeHouseholdRequest secondRequest = OrganizeHouseholdRequest.builder()
                    .name("Second Household")
                    .build();

            mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenUserCreatesFirstHouseholdThenTriesSecondViaHttp_thenSecondShouldReturnBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(any()))
                    .thenReturn(List.of(
                            User.builder().id(userId).firstName("Alice").lastName("Smith").build()
                    ));

            OrganizeHouseholdRequest firstRequest = OrganizeHouseholdRequest.builder()
                    .name("First Household")
                    .build();

            mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("First Household"));

            OrganizeHouseholdRequest secondRequest = OrganizeHouseholdRequest.builder()
                    .name("Second Household")
                    .build();

            mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetHouseholds {

        @Test
        void givenUserWithHousehold_thenShouldReturnHouseholds() throws Exception {
            UUID userId = UUID.randomUUID();
            String householdName = "Test Household";

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name(householdName)
                    .build();

            mockMvc.perform(post("/api/households")
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
                    .andExpect(jsonPath("$.name").value(householdName));

            mockMvc.perform(get("/api/households")
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
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").isNotEmpty())
                    .andExpect(jsonPath("$[0].name").value(householdName))
                    .andExpect(jsonPath("$[0].leaderId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[0].members").isArray())
                    .andExpect(jsonPath("$[0].members.length()").value(1));
        }

        @Test
        void givenUserWithNoHouseholds_thenShouldReturnEmptyList() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(get("/api/households")
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
    class PatchHousehold {

        @Test
        void givenValidPatchName_thenShouldReturnUpdatedHousehold() throws Exception {
            UUID userId = UUID.randomUUID();
            String originalName = "Original Name";
            String newName = "Updated Name";

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            OrganizeHouseholdRequest organizeRequest = OrganizeHouseholdRequest.builder()
                    .name(originalName)
                    .build();

            String createResponse = mockMvc.perform(post("/api/households")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(organizeRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID householdId = UUID.fromString(jsonMapper.readTree(createResponse).get("id").asText());

            PatchHouseholdRequest patchRequest = PatchHouseholdRequest.builder()
                    .householdName(newName)
                    .build();

            mockMvc.perform(patch("/api/households/{householdId}", householdId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(patchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(householdId.toString()))
                    .andExpect(jsonPath("$.name").value(newName))
                    .andExpect(jsonPath("$.leaderId").value(userId.toString()))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void givenNonExistentHouseholdId_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID nonExistentHouseholdId = UUID.randomUUID();

            PatchHouseholdRequest patchRequest = PatchHouseholdRequest.builder()
                    .householdName("Should Not Work")
                    .build();

            mockMvc.perform(patch("/api/households/{householdId}", nonExistentHouseholdId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(patchRequest)))
                    .andExpect(status().isNotFound());
        }
    }
}
