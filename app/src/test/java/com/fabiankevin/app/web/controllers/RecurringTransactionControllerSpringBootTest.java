package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateRecurringTransactionRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
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

            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime expectedNextOccurrenceDate = now.getDayOfMonth() < request.dayOfMonth()
                    ? now.withDayOfMonth(request.dayOfMonth())
                    : now.plusMonths(1).withDayOfMonth(request.dayOfMonth());
            int expectedRemainingDays = (int) ChronoUnit.DAYS.between(now, expectedNextOccurrenceDate);

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
                    .andExpect(jsonPath("$[0].accountId").value(account.id().toString()))
                    .andExpect(jsonPath("$[0].accountName").value("Cash Wallet"))
                    .andExpect(jsonPath("$[0].dayOfMonth").value(15))
                    .andExpect(jsonPath("$[0].nextOccurrenceDate").exists())
                    .andExpect(jsonPath("$[0].endDate").exists())
                    .andExpect(jsonPath("$[0].remainingDays").isNumber())
                    .andExpect(jsonPath("$[0].remainingDays").value(org.hamcrest.Matchers.greaterThan(0)))
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
