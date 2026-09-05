package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
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

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsControllerIntegrationTest {
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
    private JsonMapper jsonMapper;

    @Nested
    class GetStats {
        @Test
        void givenUserWithIncomeAndExpenses_thenShouldReturnStatsSummary() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            Category incomeCategory = createCategory(userId, "SALARY", TransactionType.INCOME, "salary");
            Category expenseCategory = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Account account = createAccount(userId, "Main Account");

            // Add income transaction
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(5000.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(incomeCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            // Add expense transactions
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(150.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(expenseCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(50.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(expenseCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            mockMvc.perform(get("/api/stats")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalIncome").value(5000.0))
                    .andExpect(jsonPath("$.totalExpenses").value(200.0))
                    .andExpect(jsonPath("$.totalBalance").value(4800.0))
                    .andExpect(jsonPath("$.growthPercentage").value(100.0));
        }

        @Test
        void givenUserWithIncomeAndExpenses_thenGrowthPercentageShouldReflectBalanceChange() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Eve").lastName("Wilson").build()));

            Category incomeCategory = createCategory(userId, "FREELANCE", TransactionType.INCOME, "freelance");
            Account account = createAccount(userId, "Business Account");

            // Add income to build positive balance
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(3000.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(incomeCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            mockMvc.perform(get("/api/stats")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalIncome").value(3000.0))
                    .andExpect(jsonPath("$.totalExpenses").value(0.0))
                    .andExpect(jsonPath("$.totalBalance").value(3000.0))
                    // Growth percentage should be 100% since prior balance is 0 and current is non-zero
                    .andExpect(jsonPath("$.growthPercentage").value(100.0));
        }

        @Test
        void givenUserWithNoTransactions_thenShouldReturnZeroStats() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Bob").lastName("Jones").build()));

            mockMvc.perform(get("/api/stats")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalIncome").value(0.0))
                    .andExpect(jsonPath("$.totalExpenses").value(0.0))
                    .andExpect(jsonPath("$.totalBalance").value(0.0))
                    .andExpect(jsonPath("$.growthPercentage").value(0.0));
        }

        @Test
        void givenUserWithDateFilter_thenShouldReturnFilteredStats() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Charlie").lastName("Brown").build()));

            Category expenseCategory = createCategory(userId, "DINING", TransactionType.EXPENSE, "restaurant");
            Account account = createAccount(userId, "Savings Account");

            LocalDate today = LocalDate.now();
            LocalDate lastMonth = today.minusMonths(1);

            // Add expense in current month
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(100.0)
                    .transactionDate(today)
                    .categoryId(expenseCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            // Add expense in last month
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(300.0)
                    .transactionDate(lastMonth)
                    .categoryId(expenseCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            mockMvc.perform(get("/api/stats")
                            .param("from", today.toString())
                            .param("to", today.toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalExpenses").value(100.0))
                    .andExpect(jsonPath("$.totalIncome").value(0.0));
        }

        @Test
        void givenUserWithCategoryFilter_thenShouldReturnCategoryFilteredStats() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Diana").lastName("Prince").build()));

            Category groceriesCategory = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category diningCategory = createCategory(userId, "DINING", TransactionType.EXPENSE, "restaurant");
            Account account = createAccount(userId, "Checking Account");

            // Add groceries expense
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(200.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(groceriesCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            // Add dining expense
            transactionService.addTransaction(AddTransactionCommand.builder()
                    .amount(150.0)
                    .transactionDate(LocalDate.now())
                    .categoryId(diningCategory.id())
                    .accountId(account.id())
                    .userId(userId)
                    .build());

            mockMvc.perform(get("/api/stats")
                            .param("categoryId", groceriesCategory.id().toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalExpenses").value(200.0))
                    .andExpect(jsonPath("$.totalIncome").value(0.0));
        }
    }

    // --- Test helpers ---

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
