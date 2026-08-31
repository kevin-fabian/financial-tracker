package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateTransactionRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

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
    private TransactionRepository transactionRepository;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JsonMapper jsonMapper;

    private UUID userId;
    private Account account;
    private Category category;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaTransactionRepository.deleteAll();
        categoryRepository.deleteAllByUserId(userId);
        accountRepository.deleteAllByUserId(userId);

        account = accountService.createAccount(
                CreateAccountCommand.builder()
                        .name("GCASH")
                        .userId(userId)
                        .currency(Currency.getInstance("PHP"))
                        .type(E_WALLET)
                        .build()
        );

        category = categoryService.createCategory(
                CreateCategoryCommand.builder()
                        .name("FOOD")
                        .type(TransactionType.EXPENSE)
                        .userId(userId)
                        .build()
        );
    }

    @Nested
    class CreateTransaction {

        @Test
        void givenValidRequest_thenReturnsCreatedWithTransactionResponse() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .amount(100)
                    .description("Dinner")
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .categoryId(category.id())
                    .accountId(account.id())
                    .build();

            mockMvc.perform(post("/api/transactions")
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
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/transactions/[-a-f0-9]{36}")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("Dinner"))
                    .andExpect(jsonPath("$.amount.value").value(100))
                    .andExpect(jsonPath("$.amount.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.transactionDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.account.id").value(account.id().toString()))
                    .andExpect(jsonPath("$.account.name").value("GCASH"))
                    .andExpect(jsonPath("$.account.currency").value("PHP"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("FOOD"))
                    .andExpect(jsonPath("$.category.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .amount(100)
                    .description("Dinner")
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .categoryId(category.id())
                    .accountId(account.id())
                    .build();

            mockMvc.perform(post("/api/transactions")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenMissingAmount_thenReturnsBadRequest() throws Exception {
            CreateTransactionRequest invalidRequest = CreateTransactionRequest.builder()
                    .description("Dinner")
                    .transactionDate(LocalDate.now())
                    .categoryId(category.id())
                    .accountId(account.id())
                    .build();

            mockMvc.perform(post("/api/transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNonExistentAccount_thenReturnsNotFound() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .amount(100)
                    .description("Dinner")
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .categoryId(category.id())
                    .accountId(UUID.randomUUID())
                    .build();

            mockMvc.perform(post("/api/transactions")
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
        void givenNonExistentCategory_thenReturnsNotFound() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .amount(100)
                    .description("Dinner")
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .categoryId(UUID.randomUUID())
                    .accountId(account.id())
                    .build();

            mockMvc.perform(post("/api/transactions")
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
    }

    @Nested
    class GetSummary {

        @Test
        void givenValidParams_thenReturnsSummary() throws Exception {
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(50)
                            .description("Lunch")
                            .transactionDate(LocalDate.of(2026, 1, 15))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(73)
                            .description("Dinner")
                            .transactionDate(LocalDate.of(2026, 6, 20))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/transactions/summary")
                            .param("type", "CATEGORY")
                            .param("from", "2026-01-01")
                            .param("to", "2026-12-31")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("CATEGORY"))
                    .andExpect(jsonPath("$.points").isArray())
                    .andExpect(jsonPath("$.points[0].label").value("FOOD"))
                    .andExpect(jsonPath("$.points[0].total").value(123));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/transactions/summary")
                            .param("type", "CATEGORY")
                            .param("from", "2026-01-01")
                            .param("to", "2026-12-31"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoTransactions_thenReturnsEmptySummary() throws Exception {
            mockMvc.perform(get("/api/transactions/summary")
                            .param("type", "CATEGORY")
                            .param("from", "2026-01-01")
                            .param("to", "2026-12-31")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("CATEGORY"))
                    .andExpect(jsonPath("$.points").isArray())
                    .andExpect(jsonPath("$.points").isEmpty());
        }
    }

    @Nested
    class GetTransactions {

        @Test
        void givenTransactionsExist_thenReturnsPagedResponse() throws Exception {
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("t1")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(200)
                            .description("t2")
                            .transactionDate(LocalDate.of(2026, 1, 2))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/transactions")
                            .param("page", "0")
                            .param("size", "2")
                            .param("sort", "transactionDate")
                            .param("direction", "ASC")
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
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.content[0].description").value("t1"))
                    .andExpect(jsonPath("$.content[1].description").value("t2"));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/transactions")
                            .param("page", "0")
                            .param("size", "2")
                            .param("sort", "transactionDate")
                            .param("direction", "ASC"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoTransactions_thenReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/transactions")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "transactionDate")
                            .param("direction", "ASC")
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
        void givenTransactionTypeFilter_thenReturnsFilteredTransactions() throws Exception {
            Category incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("expense")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(500)
                            .description("income")
                            .transactionDate(LocalDate.of(2026, 1, 2))
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/transactions")
                            .param("type", "EXPENSE")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "transactionDate")
                            .param("direction", "ASC")
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
                    .andExpect(jsonPath("$.content[0].description").value("expense"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    class GetTransactionById {

        @Test
        void givenExistingId_thenReturnsTransaction() throws Exception {
            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("test transaction")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/transactions/" + created.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(created.id().toString()))
                    .andExpect(jsonPath("$.description").value("test transaction"))
                    .andExpect(jsonPath("$.amount.value").value(100))
                    .andExpect(jsonPath("$.amount.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.transactionDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.account.id").value(account.id().toString()))
                    .andExpect(jsonPath("$.account.name").value("GCASH"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("FOOD"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/transactions/" + id))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNonExistentId_thenReturnsNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/transactions/" + id)
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
    class PatchTransaction {

        @Test
        void givenValidPatchRequest_thenReturnsUpdatedTransaction() throws Exception {
            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("original")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            PatchTransactionRequest request = PatchTransactionRequest.builder()
                    .description("Updated description")
                    .build();

            mockMvc.perform(patch("/api/transactions/" + created.id())
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
                    .andExpect(jsonPath("$.id").value(created.id().toString()))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.amount.value").value(100))
                    .andExpect(jsonPath("$.amount.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.transactionDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.account.id").value(account.id().toString()))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("original")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            PatchTransactionRequest request = PatchTransactionRequest.builder()
                    .description("Updated description")
                    .build();

            mockMvc.perform(patch("/api/transactions/" + created.id())
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNonExistentId_thenReturnsNotFound() throws Exception {
            PatchTransactionRequest request = PatchTransactionRequest.builder()
                    .description("Updated description")
                    .build();

            mockMvc.perform(patch("/api/transactions/" + UUID.randomUUID())
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
        void givenPatchAmountAndCategory_thenReturnsUpdatedFields() throws Exception {
            Category newCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("TRANSPORT")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("original")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            PatchTransactionRequest request = PatchTransactionRequest.builder()
                    .amount(250.0)
                    .categoryId(newCategory.id())
                    .transactionDate(LocalDate.of(2026, 3, 15))
                    .build();

            mockMvc.perform(patch("/api/transactions/" + created.id())
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
                    .andExpect(jsonPath("$.id").value(created.id().toString()))
                    .andExpect(jsonPath("$.description").value("original"))
                    .andExpect(jsonPath("$.amount.value").value(250))
                    .andExpect(jsonPath("$.amount.currency").value("PHP"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.transactionDate").value("2026-03-15"))
                    .andExpect(jsonPath("$.category.id").value(newCategory.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("TRANSPORT"));
        }
    }

    @Nested
    class DeleteTransaction {

        @Test
        void givenExistingTransaction_thenReturnsNoContent() throws Exception {
            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("to delete")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(delete("/api/transactions/" + created.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());

            transactionRepository.findById(created.id())
                    .ifPresent(t -> org.junit.jupiter.api.Assertions.fail("Transaction should be deleted"));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            Transaction created = transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("to delete")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(delete("/api/transactions/" + created.id()))
                    .andExpect(status().isUnauthorized());

            // Verify transaction still exists
            org.junit.jupiter.api.Assertions.assertNotNull(
                    transactionRepository.findById(created.id()).orElse(null),
                    "Transaction should not be deleted"
            );
        }

        @Test
        void givenNonExistentId_thenReturnsNoContent() throws Exception {
            mockMvc.perform(delete("/api/transactions/" + UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class GetTransactionsWithFilter {

        @ParameterizedTest
        @EnumSource(TransactionType.class)
        void givenTransactionTypeFilter_thenPassesTypeToService(TransactionType type) throws Exception {
            Category incomeCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100)
                            .description("expense")
                            .transactionDate(LocalDate.of(2026, 1, 1))
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(500)
                            .description("income")
                            .transactionDate(LocalDate.of(2026, 1, 2))
                            .categoryId(incomeCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/transactions")
                            .param("type", type.name())
                            .param("page", "0")
                            .param("size", "10")
                            .param("direction", "ASC")
                            .param("sort", "transactionDate")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
