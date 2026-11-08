package com.fabiankevin.app.sync_devices.web;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import com.fabiankevin.app.sync_devices.domain.models.BootstrapDirection;
import com.fabiankevin.app.sync_devices.domain.models.BootstrapStatus;
import com.fabiankevin.app.sync_devices.domain.services.SyncDeviceService;
import com.fabiankevin.app.sync_devices.persistence.JpaSyncStateRepository;
import com.fabiankevin.app.web.controllers.dtos.CreateSyncDeviceRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchSyncDeviceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JpaSyncStateRepository jpaSyncStateRepository;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private SyncDeviceService syncDeviceService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoSpyBean
    private JpaTransactionRepository jpaTransactionRepositorySpy;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaSyncStateRepository.deleteAll();
        jpaTransactionRepository.deleteAll();
        jpaCategoryRepository.deleteAll();
        jpaAccountRepository.deleteAll();
    }

    @Nested
    class PrepareSync {

        @Test
        void givenValidRequest_thenReturnsOkAndDeletesAllUserData() throws Exception {
            createTestData(userId);

            mockMvc.perform(post("/api/sync-devices/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk());

            assertThat(jpaTransactionRepository.count()).isZero();
            assertThat(jpaCategoryRepository.count()).isZero();
            assertThat(jpaAccountRepository.count()).isZero();
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/sync-devices/prepare"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenUserWithNoData_thenReturnsOk() throws Exception {
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Test").lastName("User").build()));

            mockMvc.perform(post("/api/sync-devices/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk());
        }

        @Test
        void givenTransactionDeleteFails_thenReturnsInternalServerErrorAndRollsBack() throws Exception {
            createTestData(userId);

            assertThat(jpaTransactionRepository.count()).isGreaterThan(0);
            assertThat(jpaCategoryRepository.count()).isGreaterThan(0);
            assertThat(jpaAccountRepository.count()).isGreaterThan(0);

            doThrow(new RuntimeException("database error")).when(jpaTransactionRepositorySpy).deleteAllByAccountUserId(userId);

            mockMvc.perform(post("/api/sync-devices/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isInternalServerError());

            assertThat(jpaTransactionRepository.count()).isGreaterThan(0);
            assertThat(jpaCategoryRepository.count()).isGreaterThan(0);
            assertThat(jpaAccountRepository.count()).isGreaterThan(0);
        }

        private void createTestData(UUID uid) {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("Groceries")
                            .type(EXPENSE)
                            .userId(uid)
                            .build());

            AccountSummary accountSummary = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .userId(uid)
                            .build());

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(category.id())
                            .accountId(accountSummary.account().id())
                            .userId(uid)
                            .build());
        }
    }

    @Nested
    class CreateSyncDevice {

        @Test
        void givenValidRequest_thenReturnsCreatedWithSyncState() throws Exception {
            String requestUserDeviceId = "test-device-001";
            CreateSyncDeviceRequest request = CreateSyncDeviceRequest.builder()
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/sync-devices")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("userDevice", requestUserDeviceId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.direction").value("PULL"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains(requestUserDeviceId);
            assertThat(jpaSyncStateRepository.count()).isEqualTo(1);
        }

        static Stream<CreateSyncDeviceRequest> invalidCreateSyncStateRequests() {
            return Stream.of(
                    CreateSyncDeviceRequest.builder()
                            .status(null)
                            .direction(BootstrapDirection.PULL)
                            .build(),
                    CreateSyncDeviceRequest.builder()
                            .status(BootstrapStatus.IN_PROGRESS)
                            .direction(null)
                            .build()
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidCreateSyncStateRequests")
        void givenMissingRequiredField_thenReturnsBadRequest(CreateSyncDeviceRequest request) throws Exception {
            mockMvc.perform(post("/api/sync-devices")
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
            CreateSyncDeviceRequest request = CreateSyncDeviceRequest.builder()
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .build();

            mockMvc.perform(post("/api/sync-devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenDuplicateDeviceForSameUser_shouldReturnExistingSyncState() throws Exception {
            UUID existingSyncId = createSyncStateForUser(userId, BootstrapStatus.IN_PROGRESS, BootstrapDirection.PULL, "test-device-001");

            CreateSyncDeviceRequest request = CreateSyncDeviceRequest.builder()
                    .status(BootstrapStatus.COMPLETED)
                    .direction(BootstrapDirection.PUSH)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/sync-devices")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("userDevice", "test-device-001")
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(existingSyncId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.direction").value("PULL"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("test-device-001");
            assertThat(jpaSyncStateRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    class PatchSyncDevice {

        @Test
        void givenExistingSyncState_thenReturnsUpdatedSyncState() throws Exception {
            UUID syncId = createSyncState(BootstrapStatus.IN_PROGRESS, BootstrapDirection.PULL);

            PatchSyncDeviceRequest request = PatchSyncDeviceRequest.builder()
                    .status(BootstrapStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync-devices/" + syncId)
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
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void givenNonExistentSyncState_thenReturnsNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            PatchSyncDeviceRequest request = PatchSyncDeviceRequest.builder()
                    .status(BootstrapStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync-devices/" + nonExistentId)
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
            UUID syncId = createSyncStateForUser(otherUserId, BootstrapStatus.IN_PROGRESS, BootstrapDirection.PULL, "test-device-001");

            PatchSyncDeviceRequest request = PatchSyncDeviceRequest.builder()
                    .status(BootstrapStatus.COMPLETED)
                    .build();

            mockMvc.perform(patch("/api/sync-devices/" + syncId)
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
            UUID syncId = createSyncState(BootstrapStatus.IN_PROGRESS, BootstrapDirection.PULL);

            PatchSyncDeviceRequest request = PatchSyncDeviceRequest.builder()
                    .status(null)
                    .build();

            mockMvc.perform(patch("/api/sync-devices/" + syncId)
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
            createSyncState(BootstrapStatus.COMPLETED, BootstrapDirection.PULL);
            createSyncState(BootstrapStatus.IN_PROGRESS, BootstrapDirection.PUSH);
            createSyncState(BootstrapStatus.FAILED, BootstrapDirection.PULL);

            mockMvc.perform(get("/api/sync-devices")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].status").isNotEmpty())
                    .andExpect(jsonPath("$[0].direction").isNotEmpty());
        }

        @Test
        void givenUserWithNoSyncStates_thenReturnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/sync-devices")
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
            mockMvc.perform(get("/api/sync-devices"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private static final AtomicInteger deviceCounter = new AtomicInteger(1);

    private UUID createSyncState(BootstrapStatus status, BootstrapDirection direction) {
        String userDevice = "test-device-" + deviceCounter.getAndIncrement();
        return createSyncStateForUser(userId, status, direction, userDevice);
    }

    private UUID createSyncStateForUser(UUID uid, BootstrapStatus status, BootstrapDirection direction, String userDevice) {
        CreateSyncStateCommand command = CreateSyncStateCommand.builder()
                .userId(uid)
                .userDevice(userDevice)
                .status(status)
                .direction(direction)
                .build();
        return syncDeviceService.create(command).id();
    }
}
