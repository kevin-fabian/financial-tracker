package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.party.HouseholdResponse;
import com.fabiankevin.app.web.controllers.helper.HouseholdServiceTestHelper;
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

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.fabiankevin.app.models.enums.AccountType.CREDIT_CARD;
import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class AccountControllerIntegrationTest {

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
    private AccountService accountService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private JpaRecurringTransactionRepository jpaRecurringTransactionRepository;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private HouseholdServiceTestHelper householdHelper;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaRecurringTransactionRepository.deleteAll();
        jpaTransactionRepository.deleteAll();
        jpaAccountRepository.deleteAll();
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

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

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
                    .andExpect(jsonPath("$.type").value("E_WALLET"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.totalBalance").value(0.0))
                    .andExpect(jsonPath("$.totalTransactions").value(0))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.user.initial").value("JD"));
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
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
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
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(otherUserId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
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

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

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
                    .andExpect(jsonPath("$.type").value("CREDIT_CARD"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.totalBalance").value(0.0))
                    .andExpect(jsonPath("$.totalTransactions").value(0))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.user.initial").value("JD"));
        }

        @Test
        void givenPatchAccountWithTransactions_thenReturnsUpdatedAccountWithAggregatedData() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            var incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            var expenseCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(5000.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(1500.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(500)
                            .transactionDate(LocalDate.now())
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            PatchAccountRequest request = PatchAccountRequest.builder()
                    .name("GCASH_MAIN")
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

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
                    .andExpect(jsonPath("$.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("E_WALLET"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.totalBalance").value(3000.0))
                    .andExpect(jsonPath("$.totalTransactions").value(3))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.user.initial").value("JD"));
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
        void givenAccountWithNoTransactions_thenHardDeletes() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("BDO")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.BANK_ACCOUNT)
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

            // Verify account was hard-deleted (no transactions → hard delete)
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

        @Test
        void givenAccountWithTransactions_thenSoftDeletes() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            var incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(5000.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
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

            // Verify account was soft-deleted (has transactions → active=false)
            mockMvc.perform(get("/api/accounts/" + account.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
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
        void givenExistingUserAccounts_thenReturnsPagedSummaryResponse() throws Exception {
            accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );
            accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("BDO")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.BANK_ACCOUNT)
                            .build()
            );

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/accounts?page=0&size=2&sort=name&direction=ASC")
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
                    .andExpect(jsonPath("$.content[0].user.firstName").value("John"))
                    .andExpect(jsonPath("$.content[0].user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.content[0].user.initial").value("JD"))
                    .andExpect(jsonPath("$.content[1].name").value("GCASH"))
                    .andExpect(jsonPath("$.content[1].active").value(true))
                    .andExpect(jsonPath("$.content[1].user.firstName").value("John"))
                    .andExpect(jsonPath("$.content[1].user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.content[1].user.initial").value("JD"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/accounts?page=0&size=10&sort=name&direction=ASC"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoContent_thenReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/accounts?page=0&size=10&sort=name&direction=ASC")
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

        @Test
        void givenUserWithPartyMembers_thenReturnsConsolidatedAccounts() throws Exception {
            UUID leaderId = UUID.randomUUID();
            UUID inviteeId = UUID.randomUUID();

            // Set up user mocks before party operations
            when(userClient.getUserByEmail("invitee@example.com"))
                    .thenReturn(User.builder().id(inviteeId).firstName("Bob").lastName("Jones").build());
            when(userClient.getUsersByIds(argThat(ids -> ids.contains(leaderId) && ids.contains(inviteeId))))
                    .thenReturn(
                            List.of(
                                    User.builder().id(leaderId).firstName("Alice").lastName("Smith").build(),
                                    User.builder().id(inviteeId).firstName("Bob").lastName("Jones").build()
                            )
                    );

            // Create party and invite + accept via helper
            HouseholdResponse householdResponse = householdHelper.createHouseHold(leaderId);
            householdHelper.inviteAndAccept(householdResponse.id(), leaderId, inviteeId, "invitee@example.com");

            // Create cash accounts for both users
            accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("CASH")
                            .userId(leaderId)
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.CASH)
                            .build()
            );
            accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("CASH")
                            .userId(inviteeId)
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.CASH)
                            .build()
            );

            mockMvc.perform(get("/api/accounts?page=0&size=10&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", leaderId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    // Both accounts present — order may vary when names are identical
                    .andExpect(jsonPath("$.content[0].name").value("CASH"))
                    .andExpect(jsonPath("$.content[1].name").value("CASH"))
                    .andExpect(jsonPath("$.content[*].user.id").value(
                            containsInAnyOrder(
                                    inviteeId.toString(),
                                    leaderId.toString()
                            )
                    ))
                    .andExpect(jsonPath("$.content[*].user.firstName").value(
                            containsInAnyOrder("Bob", "Alice")
                    ))
                    .andExpect(jsonPath("$.content[*].user.lastName").value(
                            containsInAnyOrder("Jones", "Smith")
                    ))
                    .andExpect(jsonPath("$.content[*].user.initial").value(
                            containsInAnyOrder("BJ", "AS")
                    ));
        }

        @Test
        void givenAccountWithIncomeAndExpenseTransactions_thenReturnsCorrectBalance() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            var incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            var expenseCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(1000.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(200)
                            .transactionDate(LocalDate.now())
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(300)
                            .transactionDate(LocalDate.now())
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/accounts?page=0&size=10&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("GCASH"))
                    .andExpect(jsonPath("$.content[0].totalBalance").value(500.0))
                    .andExpect(jsonPath("$.content[0].totalTransactions").value(3));
        }

        @Test
        void givenTransactionsFromLastMonthAndCurrentMonth_thenOnlyCurrentMonthTransactionsReflectOnAccountBalance() throws Exception {
            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .build()
            );

            var incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            var expenseCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            // Create transactions from last month — these should NOT appear in the balance
            LocalDate lastMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(15);
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(500)
                            .transactionDate(lastMonthDate)
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .transactionDate(lastMonthDate)
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            // Create transactions from the current month — these SHOULD appear in the balance
            LocalDate currentMonthDate = LocalDate.now().withDayOfMonth(10);
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(1000)
                            .transactionDate(currentMonthDate)
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(200)
                            .transactionDate(currentMonthDate)
                            .categoryId(expenseCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/accounts?page=0&size=10&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("GCASH"))
                    .andExpect(jsonPath("$.content[0].totalBalance").value(800.0))
                    .andExpect(jsonPath("$.content[0].totalTransactions").value(2));
        }
    }
}
