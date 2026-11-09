package com.fabiankevin.app.sync_devices.web;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.persistence.jpa_repositories.JpaHouseholdRepository;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.SyncDeviceService;
import com.fabiankevin.app.sync_devices.web.dtos.SyncCursorQueryRequest;
import com.fabiankevin.app.sync_devices.web.dtos.SyncPullChangesRequest;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncPullChangesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private SyncDeviceService syncDeviceService;

    @Autowired
    private SyncChangeRepository syncChangeRepository;

    @Autowired
    private HouseholdService householdService;

    @Autowired
    private JpaHouseholdRepository jpaHouseholdRepository;

    @BeforeEach
    void cleanup() {
        jpaHouseholdRepository.deleteAll();
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @MockitoBean
    private UserClient userClient;

    @Nested
    class PullChanges {

        @Test
        void givenValidCursorsWithChanges_shouldReturnScopedChanges() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Long primaryLastPulledSequence = 120L;

            // Since user has no household, getHouseholdMembersUserIds returns just the user
            // Insert a sync change with sequence > primaryLastPulledSequence
            SyncChange change = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(121L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(transactionId)
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();
            syncChangeRepository.save(change);

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(userId)
                                    .lastPulledSequence(primaryLastPulledSequence)
                                    .build()
                    ))
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
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
                    .andExpect(jsonPath("$.scopes").isArray())
                    .andExpect(jsonPath("$.scopes.length()").value(1))
                    .andExpect(jsonPath("$.scopes[0].userId").value(userId.toString()))
                    .andExpect(jsonPath("$.scopes[0].changes").isArray())
                    .andExpect(jsonPath("$.scopes[0].changes.length()").value(1))
                    .andExpect(jsonPath("$.scopes[0].changes[0].sequenceId").value(121))
                    .andExpect(jsonPath("$.scopes[0].changes[0].entityType").value("TRANSACTIONS"))
                    .andExpect(jsonPath("$.scopes[0].changes[0].entityId").value(transactionId.toString()))
                    .andExpect(jsonPath("$.scopes[0].changes[0].operation").value("CREATE"))
                    .andExpect(jsonPath("$.scopes[0].nextCursor").value(121))
                    .andExpect(jsonPath("$.scopes[0].hasMore").value(false));
        }

        @Test
        void givenValidCursorsWithNoChanges_shouldReturnEmptyScopes() throws Exception {
            UUID userId = UUID.randomUUID();
            Long primaryLastPulledSequence = 85L;

            // Insert a change with sequence <= lastPulledSequence, so it should be filtered out
            SyncChange oldChange = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(80L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();
            syncChangeRepository.save(oldChange);

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(userId)
                                    .lastPulledSequence(primaryLastPulledSequence)
                                    .build()
                    ))
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
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
                    .andExpect(jsonPath("$.scopes.length()").value(1))
                    .andExpect(jsonPath("$.scopes[0].userId").value(userId.toString()))
                    .andExpect(jsonPath("$.scopes[0].changes").isArray())
                    .andExpect(jsonPath("$.scopes[0].changes.length()").value(0))
                    .andExpect(jsonPath("$.scopes[0].nextCursor").value(85))
                    .andExpect(jsonPath("$.scopes[0].hasMore").value(false));
        }

        @Test
        void givenMultipleCursors_shouldReturnMultipleScopes() throws Exception {
            UUID userIdA = UUID.randomUUID();
            UUID userIdB = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Long primaryLastPulledSequence = 100L;

            // Insert changes for userIdA only
            SyncChange changeA = SyncChange.builder()
                    .userId(userIdA)
                    .sequenceId(121L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(transactionId)
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();
            syncChangeRepository.save(changeA);

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder().ownerId(userIdA).lastPulledSequence(120L).build(),
                            SyncCursorQueryRequest.builder().ownerId(userIdB).lastPulledSequence(85L).build()
                    ))
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userIdA)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopes.length()").value(2))
                    .andExpect(jsonPath("$.scopes[0].userId").value(userIdA.toString()))
                    .andExpect(jsonPath("$.scopes[1].userId").value(userIdB.toString()));
        }

        @Test
        void givenNullCursors_shouldReturnBadRequest() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(null)
                    .primaryLastPulledSequence(100L)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenEmptyCursors_shouldReturnBadRequest() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of())
                    .primaryLastPulledSequence(100L)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNullOwnerId_shouldReturnBadRequest() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(null)
                                    .lastPulledSequence(120L)
                                    .build()
                    ))
                    .primaryLastPulledSequence(120L)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNullPrimaryLastPulledSequence_shouldReturnBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(userId)
                                    .build()
                    ))
                    .primaryLastPulledSequence(null)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNegativePrimaryLastPulledSequence_shouldReturnBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(userId)
                                    .build()
                    ))
                    .primaryLastPulledSequence(-1L)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNoAuthentication_shouldReturnUnauthorized() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder()
                                    .ownerId(UUID.randomUUID())
                                    .build()
                    ))
                    .primaryLastPulledSequence(120L)
                    .build();

            mockMvc.perform(post("/api/sync-devices/pull")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
