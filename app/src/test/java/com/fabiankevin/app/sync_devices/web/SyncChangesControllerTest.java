package com.fabiankevin.app.sync_devices.web;

import com.fabiankevin.app.accounts.domain.models.AccountSummary;
import com.fabiankevin.app.accounts.domain.services.AccountService;
import com.fabiankevin.app.accounts.domain.services.commands.CreateAccountCommand;
import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.persistence.jpa_repositories.JpaHouseholdRepository;
import com.fabiankevin.app.services.EntitlementService;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.web.dtos.SyncCursorQueryRequest;
import com.fabiankevin.app.sync_devices.web.dtos.SyncPullChangesRequest;
import com.fabiankevin.app.web.controllers.dtos.household.HouseholdResponse;
import com.fabiankevin.app.web.controllers.helper.HouseholdServiceTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncChangesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private SyncChangeRepository syncChangeRepository;

    @Autowired
    private JpaHouseholdRepository jpaHouseholdRepository;

    @Autowired
    @Qualifier("syncChangeAwareAccountService")
    private AccountService accountService;

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

    @MockitoBean
    private EntitlementService entitlementService;

    @Autowired
    private HouseholdServiceTestHelper householdServiceTestHelper;

    @Nested
    class PullChanges {

        @Test
        void givenCurrentUserOnly_shouldReturnScopedChanges() throws Exception {
            UUID userId = UUID.randomUUID();
            Long primaryLastPulledSequence = 0L;

            // Create account through SyncChangeAwareAccountService — this automatically registers a sync change
            CreateAccountCommand createAccountCommand = CreateAccountCommand.builder()
                    .id(UUID.randomUUID())
                    .name("Test Account")
                    .currency(Currency.getInstance("USD"))
                    .type(com.fabiankevin.app.accounts.domain.models.AccountType.E_WALLET)
                    .userId(userId)
                    .build();

            AccountSummary createdAccount = accountService.createAccount(createAccountCommand);
            UUID accountId = createdAccount.account().id();

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
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
                    .andExpect(jsonPath("$.scopes[0].changes[0].entityType").value("ACCOUNTS"))
                    .andExpect(jsonPath("$.scopes[0].changes[0].entityId").value(accountId.toString()))
                    .andExpect(jsonPath("$.scopes[0].changes[0].operation").value("CREATE"))
                    .andExpect(jsonPath("$.scopes[0].changes[0].payload").isNotEmpty());
        }

        @Test
        void givenValidCursorsWithNoChanges_shouldReturnEmptyScopes() throws Exception {
            UUID userId = UUID.randomUUID();
            Long primaryLastPulledSequence = 85L;

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
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
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
                    .andExpect(jsonPath("$.scopes.length()").value(0));
        }

        @Test
        void givenValidHouseholdMembersAndCursorWithMemberUserIds_shouldReturnMultipleScopes() throws Exception {
            UUID userIdA = UUID.randomUUID();
            UUID userIdB = UUID.randomUUID();
            UUID transactionIdA = UUID.randomUUID();
            UUID transactionIdB = UUID.randomUUID();
            Long primaryLastPulledSequence = 100L;

            // Mock user lookup for leader creating household
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.size() == 1 && ids.get(0).equals(userIdA))))
                    .thenReturn(List.of(User.builder().id(userIdA).firstName("Alice").lastName("Smith").build()));

            // Create household with both users as members
            HouseholdResponse household =
                    householdServiceTestHelper.createHouseHold(userIdA);
            UUID householdId = household.id();

            // Mock user lookup for member email (invitation send)
            when(userClient.getUserByEmail("userb@example.com"))
                    .thenReturn(User.builder().id(userIdB).firstName("Bob").lastName("Jones").build());

            // Mock user lookup for both users (for invitation accept)
            when(userClient.getUsersByIds(argThat(ids -> ids != null && ids.contains(userIdA) && ids.contains(userIdB))))
                    .thenReturn(List.of(
                            User.builder().id(userIdA).firstName("Alice").lastName("Smith").build(),
                            User.builder().id(userIdB).firstName("Bob").lastName("Jones").build()
                    ));

            // Invite and accept (uses helper internally)
            householdServiceTestHelper.inviteAndAccept(householdId, userIdA, userIdB, "userb@example.com");

            // Insert changes for both users
            SyncChange changeA = SyncChange.builder()
                    .userId(userIdA)
                    .sequenceId(121L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(transactionIdA)
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();
            syncChangeRepository.save(changeA);

            SyncChange changeB = SyncChange.builder()
                    .userId(userIdB)
                    .sequenceId(90L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(transactionIdB)
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();
            syncChangeRepository.save(changeB);

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder().ownerId(userIdB).lastPulledSequence(85L).build()
                    ))
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
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
                    .andExpect(jsonPath("$.scopes").isArray())
                    .andExpect(jsonPath("$.scopes.length()").value(2))
                    .andExpect(jsonPath("$.scopes[0].userId").value(userIdA.toString()))
                    .andExpect(jsonPath("$.scopes[0].changes.length()").value(1))
                    .andExpect(jsonPath("$.scopes[0].changes[0].sequenceId").value(121))
                    .andExpect(jsonPath("$.scopes[1].userId").value(userIdB.toString()))
                    .andExpect(jsonPath("$.scopes[1].changes.length()").value(1))
                    .andExpect(jsonPath("$.scopes[1].changes[0].sequenceId").value(90));
        }

        @Test
        void givenNullCursors_shouldReturnOkWithEmptyScopes() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(null)
                    .primaryLastPulledSequence(100L)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopes").isArray())
                    .andExpect(jsonPath("$.scopes.length()").value(0));
        }

        @Test
        void givenEmptyCursors_shouldReturnOkWithEmptyScopes() throws Exception {
            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of())
                    .primaryLastPulledSequence(100L)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopes").isArray())
                    .andExpect(jsonPath("$.scopes.length()").value(0));
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

            mockMvc.perform(post("/api/sync-changes")
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

            mockMvc.perform(post("/api/sync-changes")
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

            mockMvc.perform(post("/api/sync-changes")
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

            mockMvc.perform(post("/api/sync-changes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenCursorWithOtherUserIdsButNoHousehold_shouldReturnForbidden() throws Exception {
            UUID userIdA = UUID.randomUUID();
            UUID userIdB = UUID.randomUUID();
            Long primaryLastPulledSequence = 100L;

            SyncPullChangesRequest request = SyncPullChangesRequest.builder()
                    .cursors(List.of(
                            SyncCursorQueryRequest.builder().ownerId(userIdA).lastPulledSequence(120L).build(),
                            SyncCursorQueryRequest.builder().ownerId(userIdB).lastPulledSequence(85L).build()
                    ))
                    .primaryLastPulledSequence(primaryLastPulledSequence)
                    .build();

            mockMvc.perform(post("/api/sync-changes")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userIdA)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
