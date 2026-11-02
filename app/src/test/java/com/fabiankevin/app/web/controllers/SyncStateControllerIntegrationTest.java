package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSyncStateRepository;
import com.fabiankevin.app.services.SyncStateService;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateSyncStateRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchSyncStateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncStateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JpaSyncStateRepository jpaSyncStateRepository;

    @Autowired
    private SyncStateService syncStateService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @MockitoBean
    private UserClient userClient;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaSyncStateRepository.deleteAll();
    }

    @Nested
    class CreateSyncState {

        @Test
        void givenValidRequest_thenReturnsCreatedWithSyncState() throws Exception {
            CreateSyncStateRequest request = CreateSyncStateRequest.builder()
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.PENDING)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/sync")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userDevice").value("iphone-15"))
                    .andExpect(jsonPath("$.resourceType").value("ACCOUNTS"))
                    .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("iphone-15");
            assertThat(jpaSyncStateRepository.count()).isEqualTo(1);
        }

        @Test
        void givenMissingResourceType_thenReturnsBadRequest() throws Exception {
            CreateSyncStateRequest request = CreateSyncStateRequest.builder()
                    .userDevice("iphone-15")
                    .resourceType(null)
                    .syncStatus(SyncStatus.PENDING)
                    .build();

            mockMvc.perform(post("/api/sync")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            CreateSyncStateRequest request = CreateSyncStateRequest.builder()
                    .userDevice("iphone-15")
                    .resourceType(ResourceType.ACCOUNTS)
                    .syncStatus(SyncStatus.PENDING)
                    .build();

            mockMvc.perform(post("/api/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PatchSyncState {

        @Test
        void givenExistingSyncState_thenReturnsUpdatedSyncState() throws Exception {
            UUID syncId = createSyncState(ResourceType.ACCOUNTS, SyncStatus.PENDING);

            PatchSyncStateRequest request = PatchSyncStateRequest.builder()
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync/" + syncId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(syncId.toString()))
                    .andExpect(jsonPath("$.syncStatus").value("COMPLETED"));
        }

        @Test
        void givenNonExistentSyncState_thenReturnsNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            PatchSyncStateRequest request = PatchSyncStateRequest.builder()
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync/" + nonExistentId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenSyncStateOwnedByDifferentUser_thenReturnsNotFound() throws Exception {
            UUID otherUserId = UUID.randomUUID();
            UUID syncId = createSyncStateForUser(otherUserId, ResourceType.ACCOUNTS, SyncStatus.PENDING);

            PatchSyncStateRequest request = PatchSyncStateRequest.builder()
                    .syncStatus(SyncStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync/" + syncId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenMissingSyncStatus_thenReturnsBadRequest() throws Exception {
            UUID syncId = createSyncState(ResourceType.ACCOUNTS, SyncStatus.PENDING);

            PatchSyncStateRequest request = PatchSyncStateRequest.builder()
                    .syncStatus(null)
                    .build();

            mockMvc.perform(patch("/api/sync/" + syncId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetAllSyncStates {

        @Test
        void givenUserWithSyncStates_thenReturnsAllSyncStates() throws Exception {
            createSyncState(ResourceType.ACCOUNTS, SyncStatus.PENDING);
            createSyncState(ResourceType.CATEGORIES, SyncStatus.COMPLETED);
            createSyncState(ResourceType.TRANSACTIONS, SyncStatus.IN_PROGRESS);

            mockMvc.perform(get("/api/sync")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].resourceType").isNotEmpty())
                    .andExpect(jsonPath("$[0].syncStatus").isNotEmpty());
        }

        @Test
        void givenUserWithNoSyncStates_thenReturnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/sync")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/sync"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private UUID createSyncState(ResourceType resourceType, SyncStatus syncStatus) {
        return createSyncStateForUser(userId, resourceType, syncStatus);
    }

    private UUID createSyncStateForUser(UUID uid, ResourceType resourceType, SyncStatus syncStatus) {
        CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                .userId(uid)
                .userDevice("iphone-15")
                .resourceType(resourceType)
                .syncStatus(syncStatus)
                .build();
        return syncStateService.createSyncState(command).id();
    }
}
