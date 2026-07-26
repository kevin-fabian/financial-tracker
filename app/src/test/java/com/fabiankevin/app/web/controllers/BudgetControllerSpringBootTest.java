package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.web.controllers.dtos.budgets.CreateBudgetRequest;
import com.fabiankevin.app.web.controllers.dtos.budgets.PatchBudgetRequest;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetControllerSpringBootTest {
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
    private CategoryService categoryService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private JsonMapper jsonMapper;

    @Nested
    class GetBudgets {
        @Test
        void givenNoBudgets_thenShouldReturnEmpty() throws Exception {
            UUID userId = UUID.randomUUID();
            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void givenBudgetWithTransactions_thenShouldReturnSummaryWithSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            createTransaction(account, category, 150.0);
            createTransaction(account, category, 50.0);
            createBudget(userId, category, BudgetPeriod.MONTHLY, 500.0);

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].lastUpdatedBy").value(userId.toString()))
                    .andExpect(jsonPath("$[0].lastUpdatedByName").value("John Doe"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$[0].categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].allocated").value(500.0))
                    .andExpect(jsonPath("$[0].spent").value(200.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(40.0));
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class CreateBudget {
        @Test
        void givenValidRequest_thenShouldReturnCreated() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdatedByName").value("John Doe"))
                    .andExpect(jsonPath("$.lastUpdatedBy").value(userId.toString()))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.spent").value(0.0))
                    .andExpect(jsonPath("$.spentPercentage").value(0.0));
        }

        @Test
        void givenCategoryWithExistingTransactions_thenShouldReturnSummaryWithSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Cash Wallet");
            createTransaction(account, category, 150.0);
            createTransaction(account, category, 50.0);

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdatedByName").value("John Doe"))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.spent").value(200.0))
                    .andExpect(jsonPath("$.spentPercentage").value(40.0));
        }

        @Test
        void givenExistingBudgetFromLastMonth_thenCreatingNewBudgetForSameCategoryShouldBeAllowed() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            Instant lastMonth = Instant.now().atZone(ZoneOffset.UTC).minusMonths(1).toInstant();
            Budget backdatedBudget = Budget.builder()
                    .userId(userId)
                    .lastUpdatedBy(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(300.0)
                    .createdAt(lastMonth)
                    .updatedAt(lastMonth)
                    .build();
            budgetRepository.save(backdatedBudget);

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        void givenCategoryNotFound_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class PatchBudget {
        @Test
        void givenValidRequest_thenShouldReturnUpdatedBudget() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budget = createBudget(userId, category, BudgetPeriod.MONTHLY, 500.0);

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .period(BudgetPeriod.YEARLY)
                    .allocated(1000.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.lastUpdatedBy").value(userId.toString()))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("YEARLY"))
                    .andExpect(jsonPath("$.categoryId").value(category.id().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(1000.0))
                    .andExpect(jsonPath("$.spent").value(0.0))
                    .andExpect(jsonPath("$.spentPercentage").value(0.0));
        }

        @Test
        void givenBudgetNotFound_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DeleteBudget {
        @Test
        void givenExistingBudget_thenShouldReturnNoContent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budget = createBudget(userId, category, BudgetPeriod.MONTHLY, 500.0);

            mockMvc.perform(delete("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + id))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + id)
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isForbidden());
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

    private void createTransaction(Account account, Category category, double amount) {
        transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(Amount.of(amount, "USD"))
                .transactionDate(LocalDate.now())
                .categoryId(category.id())
                .accountId(account.id())
                .userId(account.userId())
                .build());
    }

    private BudgetSummary createBudget(UUID userId, Category category, BudgetPeriod period, double allocated) {
        return budgetService.createBudget(CreateBudgetCommand.builder()
                .userId(userId)
                .period(period)
                .categoryId(category.id())
                .allocated(allocated)
                .build());
    }
}
