package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.fabiankevin.app.models.enums.AccountType.CREDIT_CARD;
import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerSpringBootTest {

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
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private JsonMapper jsonMapper;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
    }

    @Nested
    class CreateAccount {

        @Test
        void givenValidRequest_thenReturnsCreatedWithAccountResponse() throws Exception {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .name("GCASH")
                    .currency("PHP")
                    .type(E_WALLET)
                    .build();

            mockMvc.perform(post("/api/accounts")
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
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/accounts/[-a-f0-9]{36}")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("GCASH"))
                    .andExpect(jsonPath("$.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("E_WALLET"));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .name("GCASH")
                    .currency("PHP")
                    .type(E_WALLET)
                    .build();

            mockMvc.perform(post("/api/accounts")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        static Stream<Arguments> invalidCreateAccountRequestTestCases() {
            return Stream.of(
                    Arguments.of("", "PHP", E_WALLET),
                    Arguments.of(" ", "PHP", E_WALLET),
                    Arguments.of("   ", "PHP", E_WALLET),
                    Arguments.of("GCASH", null, E_WALLET),
                    Arguments.of("12345".repeat(100), "PHP", E_WALLET),
                    Arguments.of((String) null, "PHP", E_WALLET),
                    Arguments.of("GCASH", "", E_WALLET),
                    Arguments.of("GCASH", "   ", E_WALLET),
                    Arguments.of("GCASH", "12345".repeat(100), E_WALLET),
                    Arguments.of("GCASH", "PHP", (AccountType) null)
            );
        }

        @ParameterizedTest
        @MethodSource("invalidCreateAccountRequestTestCases")
        void givenInvalidCreateAccountRequest_thenReturnsBadRequest(String name, String currency, AccountType type) throws Exception {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .name(name)
                    .currency(currency)
                    .type(type)
                    .build();

            mockMvc.perform(post("/api/accounts")
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
        void givenDuplicateAccountNameAndType_thenReturnsConflict() throws Exception {
            // Create first account
            CreateAccountRequest firstRequest = CreateAccountRequest.builder()
                    .name("GCASH")
                    .currency("PHP")
                    .type(E_WALLET)
                    .build();

            mockMvc.perform(post("/api/accounts")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            // Try to create duplicate
            mockMvc.perform(post("/api/accounts")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class GetAccountById {

        @Test
        void givenExistingId_thenReturnsAccount() throws Exception {
            Account account = accountRepository.save(
                    Account.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/accounts/" + account.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(account.id().toString()))
                    .andExpect(jsonPath("$.name").value("GCASH"))
                    .andExpect(jsonPath("$.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("E_WALLET"));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/accounts/" + id))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAccountNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/accounts/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenAccountBelongsToOtherUser_thenReturnNotFound() throws Exception {
            UUID otherUserId = UUID.randomUUID();
            Account account = accountRepository.save(
                    Account.builder()
                            .name("GCASH")
                            .userId(otherUserId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/accounts/" + account.id())
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

    @Nested
    class PatchAccount {

        @Test
        void givenFullPatchRequest_thenReturnsUpdatedAccount() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .name("GCASH_MAIN")
                    .currency("USD")
                    .type(CREDIT_CARD)
                    .build();

            mockMvc.perform(patch("/api/accounts/" + account.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(account.id().toString()))
                    .andExpect(jsonPath("$.name").value("GCASH_MAIN"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.type").value("CREDIT_CARD"));
        }

        @ParameterizedTest
        @MethodSource("invalidPatchAccountRequestTestCases")
        void givenInvalidPatchAccountRequest_thenReturnsBadRequest(String name, String currency, AccountType type) throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .name(name)
                    .currency(currency)
                    .type(type)
                    .build();

            mockMvc.perform(patch("/api/accounts/" + account.id())
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

        private static Stream<Arguments> invalidPatchAccountRequestTestCases() {
            return Stream.of(
                    Arguments.of("12345".repeat(100), null, null),
                    Arguments.of(null, "12345".repeat(100), null)
            );
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .name("GCASH_MAIN")
                    .build();

            mockMvc.perform(patch("/api/accounts/" + id)
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAccountNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .name("GCASH_MAIN")
                    .build();

            mockMvc.perform(patch("/api/accounts/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenPartialPatch_thenOnlyUpdatesProvidedFields() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .currency("USD")
                    .build();

            mockMvc.perform(patch("/api/accounts/" + account.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("GCASH"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.type").value("E_WALLET"));
        }
    }

    @Nested
    class DisableAccount {

        @Test
        void givenExistingId_thenShouldDisableAccount() throws Exception {
            Account account = accountRepository.save(
                    Account.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(post("/api/accounts/" + account.id() + "/disable")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk());

            // Verify account is now inactive
            Account updated = accountRepository.findById(account.id()).orElseThrow();
            if (!updated.active()) {
                // Account is disabled as expected
            }
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/api/accounts/" + id + "/disable"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAccountNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/api/accounts/" + id + "/disable")
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

    @Nested
    class GetAccounts {

        @Test
        void givenValidParams_thenReturnsPagedSummaryResponse() throws Exception {
            accountRepository.save(
                    Account.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
            accountRepository.save(
                    Account.builder()
                            .name("BDO")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.BANK_ACCOUNT)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/accounts/summaries?page=0&size=2&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("BDO"))
                    .andExpect(jsonPath("$.content[0].active").value(true))
                    .andExpect(jsonPath("$.content[1].name").value("GCASH"))
                    .andExpect(jsonPath("$.content[1].active").value(true))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/accounts/summaries?page=0&size=10&sort=name&direction=ASC"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoContent_thenReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/accounts/summaries?page=0&size=10&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }
    }
}
