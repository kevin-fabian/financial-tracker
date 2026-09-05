package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import org.junit.jupiter.api.DisplayName;
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

            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name(householdName)
                    .build();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

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

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Creating a household with null or empty name returns 201")
        void givenRequestWithNullOrEmptyName_thenShouldReturnCreated(String name) throws Exception {
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
        void givenUserWithExistingHousehold_thenShouldReturnBadRequest() throws Exception {
            UUID leaderUserId = UUID.randomUUID();

            when(userClient.getUsersByIds(any()))
                    .thenReturn(List.of(
                            User.builder().id(leaderUserId).firstName("Alice").lastName("Smith").build()
                    ));

            // Setup: create first household via service for leaderUserId
            HouseholdSummary existingHousehold = householdService.organize(
                    OrganizeHouseholdCommand.builder()
                            .leaderId(leaderUserId)
                            .householdName("First Household")
                            .build());
            assertNotNull(existingHousehold.id());

            // Act: try to create second household via HTTP for the same user
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

            // Act 1: create first household via HTTP (should succeed)
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

            // Act 2: try to create second household via HTTP for the same user (should fail)
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

        @Test
        @DisplayName("Creating a household with a name exceeding 100 characters returns 400")
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
    }
}
