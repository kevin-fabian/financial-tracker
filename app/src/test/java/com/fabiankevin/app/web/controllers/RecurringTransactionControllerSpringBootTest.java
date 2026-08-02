package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.persistence.RecurringTransactionRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateRecurringTransactionRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchRecurringTransactionRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurringTransactionControllerSpringBootTest {

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
    private RecurringTransactionService recurringTransactionService;

    @MockitoSpyBean
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private JsonMapper jsonMapper;

    @Nested
    class Create {

        @Test
        void givenValidRequest_thenReturnsCreatedWithSummary() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            LocalDate today = LocalDate.now();
            LocalDate expectedNextOccurrenceDate = today.getDayOfMonth() < request.dayOfMonth()
                    ? today.withDayOfMonth(request.dayOfMonth())
                    : today.plusMonths(1).withDayOfMonth(request.dayOfMonth());
            int expectedRemainingDays = (int) ChronoUnit.DAYS.between(today, expectedNextOccurrenceDate);

            mockMvc.perform(post("/api/recurring-transactions")
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
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.accountName").value("Cash Wallet"))
                    .andExpect(jsonPath("$.dayOfMonth").value(15))
                    .andExpect(jsonPath("$.remainingDays").value(expectedRemainingDays))
                    .andExpect(jsonPath("$.transactionStatus").value("UPCOMING"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"));
        }

        @Test
        void givenNoEndDateFlag_thenReturnsCreatedWithEndDateNull() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(true)
                    .dayOfMonth(0)
                    .durationMonths(6)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/recurring-transactions")
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
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.endDate").isEmpty())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void givenVariableAmountFlag_thenReturnsCreatedWithAmountZero() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(0)
                    .variableAmount(true)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/recurring-transactions")
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
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.amount").value(0.0))
                    .andExpect(jsonPath("$.variableAmount").value(true))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(UUID.randomUUID())
                    .accountId(UUID.randomUUID())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenNonExistentAccount_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(UUID.randomUUID())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
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
            UUID userId = UUID.randomUUID();
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(UUID.randomUUID())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
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
        void givenDayOfMonthOutOfRange_thenReturnsBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(50)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
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
        void givenVariableAmountFlagWithNonZeroAmount_thenReturnsBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(true)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
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

    @Nested
    class GetRecurringTransactions {

        @Test
        void givenRecurringTransactionsExist_thenReturnsListOfSummaries() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").isNotEmpty())
                    .andExpect(jsonPath("$[0].description").value("Monthly subscription"))
                    .andExpect(jsonPath("$[0].amount").value(15.99))
                    .andExpect(jsonPath("$[0].variableAmount").value(false))
                    .andExpect(jsonPath("$[0].categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$[0].categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].transactionType").value("EXPENSE"))
                    .andExpect(jsonPath("$[0].accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$[0].accountName").value("Cash Wallet"))
                    .andExpect(jsonPath("$[0].dayOfMonth").value(15))
                    .andExpect(jsonPath("$[0].nextOccurrenceDate").exists())
                    .andExpect(jsonPath("$[0].endDate").exists())
                    .andExpect(jsonPath("$[0].remainingDays").isNumber())
                    .andExpect(jsonPath("$[0].remainingDays").value(Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$[0].transactionStatus").value("UPCOMING"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].initial").value("JD"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists());
        }

        @Test
        void givenNoRecurringTransactions_thenReturnsEmptyList() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(get("/api/recurring-transactions")
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
    }

    @Nested
    class DeleteRecurringTransaction {

        @Test
        void givenExistingRecurringTransaction_thenReturnsNoContent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/recurring-transactions")
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

            String id = jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/recurring-transactions/{id}", id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenNonExistentRecurringTransaction_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/recurring-transactions/{id}", UUID.randomUUID())
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
    class PatchRecurringTransaction {

        @Test
        void givenExistingRecurringTransaction_thenReturnsUpdatedSummary() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            Category newCategory = createCategory(userId, "ENTERTAINMENT", TransactionType.EXPENSE, "movie");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .description("Updated subscription")
                    .amount(25.99)
                    .categoryId(newCategory.id())
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.description").value("Updated subscription"))
                    .andExpect(jsonPath("$.amount").value(25.99))
                    .andExpect(jsonPath("$.categoryId").value(newCategory.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("ENTERTAINMENT"))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.accountName").value("Cash Wallet"))
                    .andExpect(jsonPath("$.dayOfMonth").value(15))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"));
        }

        @Test
        void givenNonExistentRecurringTransaction_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .description("Updated subscription")
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", UUID.randomUUID())
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

        @Test
        void givenExistingRecurringTransactionWithEndDate_thenPatchNoEndDateReturnsEndDateNull() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.endDate").exists())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .noEndDate(true)
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.endDate").isEmpty())
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.dayOfMonth").value(15))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"));
        }

        @Test
        void givenExistingRecurringTransactionWithNoEndDate_thenPatchNoEndDateFalseWithoutDurationMonthsReturnsBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(true)
                    .dayOfMonth(0)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.endDate").isEmpty())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .noEndDate(false)
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(patchRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenExistingRecurringTransactionWithNonZeroAmount_thenPatchVariableAmountTrueWithoutAmountZero_thenReturnsBadRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .variableAmount(true)
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(patchRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenExistingRecurringTransactionWithNoEndDate_thenPatchNoEndDateFalseWithDurationMonthsWithEndDateNotNull_thenReturnsOk() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(true)
                    .dayOfMonth(0)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.endDate").isEmpty())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .noEndDate(false)
                    .durationMonths(3)
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.endDate").exists())
                    .andExpect(jsonPath("$.endDate").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"));
        }

        @Test
        void givenExistingRecurringTransaction_thenPatchDayOfMonthOnly_thenReturnsUpdatedNextOccurrenceDate_thenReturnsOk() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
            String originalNextOccurrenceDate = jsonMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("nextOccurrenceDate").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .dayOfMonth(25)
                    .build();

            MvcResult patchResult = mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.dayOfMonth").value(25))
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"))
                    .andReturn();

            String updatedNextOccurrenceDate = jsonMapper.readTree(patchResult.getResponse().getContentAsString())
                    .get("nextOccurrenceDate").asText();
            org.junit.jupiter.api.Assertions.assertNotEquals(originalNextOccurrenceDate, updatedNextOccurrenceDate);
        }

        @Test
        void givenExistingRecurringTransaction_thenPatchNonExistentCategory_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .categoryId(UUID.randomUUID())
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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

        @Test
        void givenExistingRecurringTransaction_thenPatchNonExistentAccount_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder()
                    .accountId(UUID.randomUUID())
                    .build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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

        @Test
        void givenExistingRecurringTransaction_thenPatchEmptyBody_thenReturnsUnchangedSummary_thenReturnsOk() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateRecurringTransactionRequest createRequest = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(category.id())
                    .accountId(account.id())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = jsonMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            PatchRecurringTransactionRequest patchRequest = PatchRecurringTransactionRequest.builder().build();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/recurring-transactions/{id}", id)
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
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$.accountName").value("Cash Wallet"))
                    .andExpect(jsonPath("$.dayOfMonth").value(15))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"));
        }
    }

    @Nested
    class ProcessDueRecurringTransactions {
        @Autowired
        private JpaTransactionRepository jpaTransactionRepository;

        @BeforeEach
        void beforeEach() {
            jpaTransactionRepository.deleteAll();
        }

        @Test
        void givenValidClientCredentials_thenTriggerProcessDueReturnsAccepted() throws Exception {
            mockMvc.perform(post("/api/recurring-transactions/process-due")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("zeny:operator"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isAccepted());
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            mockMvc.perform(post("/api/recurring-transactions/process-due"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenDueRecurringTransaction_thenProcessDueAdvancesNextOccurrenceDateToNextMonth() {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
            int dayOfMonth = today.plusMonths(1).lengthOfMonth() >= 15 ? 15 : today.plusMonths(1).lengthOfMonth();
            LocalDate pastDate = today.minusDays(1);

            RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                    .userId(userId)
                    .description("Due transaction")
                    .amount(10.0)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(dayOfMonth)
                    .nextOccurrenceDate(pastDate)
                    .endDate(null)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

            recurringTransactionService.processDueRecurringTransactions();

            verify(recurringTransactionRepository,
                    timeout(Duration.ofSeconds(10).toMillis()).atLeastOnce()).saveAll(anyList());

            RecurringTransaction updated = recurringTransactionRepository.findByIdAndUserId(saved.id(), userId).orElseThrow();
            LocalDate expectedNextOccurrenceDate = today.plusMonths(1).withDayOfMonth(dayOfMonth);
            assertEquals(expectedNextOccurrenceDate, updated.nextOccurrenceDate());
        }

        @Test
        void givenProcessTwice_thenShouldExecuteOnlyOnce() {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
            int dayOfMonth = today.plusMonths(1).lengthOfMonth() >= 15 ? 15 : today.plusMonths(1).lengthOfMonth();
            LocalDate pastDate = today.minusDays(1);

            RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                    .userId(userId)
                    .description("Due transaction")
                    .amount(10.0)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(dayOfMonth)
                    .nextOccurrenceDate(pastDate)
                    .endDate(null)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            recurringTransactionRepository.save(recurringTransaction);

            recurringTransactionService.processDueRecurringTransactions();
            verify(recurringTransactionRepository, timeout(Duration.ofSeconds(10).toMillis()).atLeastOnce()).saveAll(anyList());
            recurringTransactionService.processDueRecurringTransactions();
            verify(recurringTransactionRepository, timeout(Duration.ofSeconds(10).toMillis()).times(1)).saveAll(anyList());
        }

        @Test
        void givenTenDueRecurringTransactions_thenProcessDueCreatesTenTransactions() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            LocalDate pastDate = LocalDate.now().minusDays(1);

            for (int i = 0; i < 10; i++) {
                RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                        .userId(userId)
                        .description("Due transaction " + i)
                        .amount(10.0 + i)
                        .variableAmount(false)
                        .category(category)
                        .account(account)
                        .dayOfMonth(15)
                        .nextOccurrenceDate(pastDate)
                        .endDate(null)
                        .status(RecurringTransactionStatus.ACTIVE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                recurringTransactionRepository.save(recurringTransaction);
            }

            recurringTransactionService.processDueRecurringTransactions();

            // Wait for async processing to complete
            Thread.sleep(2000);

            mockMvc.perform(get("/api/transactions")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.content.length()").value(10));
        }
    }

    private Category createCategory(UUID userId, String name, TransactionType type, String icon) {
        return categoryService.createCategory(CreateCategoryCommand.builder()
                .name(name)
                .type(type)
                .icon(icon)
                .userId(userId)
                .build());
    }

    private Account createAccount(UUID userId, String name) {
        return accountService.createAccount(CreateAccountCommand.builder()
                .name(name)
                .currency(Currency.getInstance("USD"))
                .type(AccountType.CASH)
                .userId(userId)
                .build());
    }
}
