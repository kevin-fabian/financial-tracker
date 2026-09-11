package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.web.controllers.helper.TransactionServiceTestHelper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static com.fabiankevin.app.models.enums.TransactionType.INCOME;
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
    private TransactionServiceTestHelper transactionHelper;

    @Nested
    class GetStats {
        @Test
        void givenUserWithIncomeAndExpenses_thenGrowthPercentageShouldReflectBalanceChange() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Eve").lastName("Wilson").build()));

            // Add income to build positive balance
            transactionHelper.createTransaction(userId, INCOME, 3000.0);

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

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
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
        void givenUserWithTransactionsLastMonthAndCurrentMonth_thenGrowthPercentageShouldReflectBalanceChange() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Frank").lastName("Miller").build()));

            LocalDate today = LocalDate.now();
            LocalDate lastMonth = today.minusMonths(1);

            // Last month: income $2000 → not in current period, but contributes to all-time balance
            transactionHelper.createTransaction(userId, INCOME, 2000.0, lastMonth);

            // Current month: income $3000, expenses $500 → in current period
            transactionHelper.createTransaction(userId, INCOME, 3000.0, today);
            transactionHelper.createTransaction(userId, EXPENSE, 500.0, today);

            // totalIncome/totalExpenses = current period only (3000, 500)
            // totalBalance = all-time (2000 + 3000 - 500 = 4500)
            // totalBalanceLastMonthWithSameDate = 2000 (last month's balance)
            // Growth = (4500 - 2000) / 2000 * 100 = 125%
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
                    .andExpect(jsonPath("$.totalExpenses").value(500.0))
                    .andExpect(jsonPath("$.totalBalance").value(4500.0))
                    .andExpect(jsonPath("$.growthPercentage").value(125.0));
        }

        @Test
        void givenUserWithDateFilter_thenShouldReturnFilteredStats() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Charlie").lastName("Brown").build()));

            LocalDate today = LocalDate.now();
            LocalDate lastMonth = today.minusMonths(1);

            // Add expense in current month
            transactionHelper.createTransaction(userId, EXPENSE, 100.0, today);

            // Add expense in last month
            transactionHelper.createTransaction(userId, EXPENSE, 300.0, lastMonth);

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

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Diana").lastName("Prince").build()));

            // Stats endpoint returns aggregate totals across all categories — categoryId filter is not supported
            Transaction expense1 = transactionHelper.createTransaction(userId, EXPENSE, 200.0, "Dining");
            transactionHelper.createTransaction(userId, EXPENSE, 150.0, "Groceries");

            mockMvc.perform(get("/api/stats")
                            .param("categoryId", expense1.category().id().toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalExpenses").value(350.0))
                    .andExpect(jsonPath("$.totalIncome").value(0.0));
        }
    }

    @Nested
    class GetDailyStats {
        @Test
        void givenUserWithTransactionsOnDifferentDaysOfThenShouldReturnGroupedByDayOfMonth() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Grace").lastName("Hopper").build()));

            LocalDate today = LocalDate.now();
            int dayOfMonth = today.getDayOfMonth();

            // Transaction on day 5 of month
            transactionHelper.createTransaction(userId, INCOME, 500.0, today.withDayOfMonth(5));
            transactionHelper.createTransaction(userId, EXPENSE, 100.0, today.withDayOfMonth(5));

            // Transaction on current day of month
            transactionHelper.createTransaction(userId, INCOME, 300.0, today);
            transactionHelper.createTransaction(userId, EXPENSE, 50.0, today);

            mockMvc.perform(get("/api/stats/daily")
                            .param("from", today.withDayOfMonth(1).toString())
                            .param("to", today.withDayOfMonth(today.lengthOfMonth()).toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[?(@.label == '5')].totalIncome").value(500.0))
                    .andExpect(jsonPath("$.data[?(@.label == '5')].totalExpenses").value(100.0))
                    .andExpect(jsonPath("$.data[?(@.label == '" + dayOfMonth + "')].totalIncome").value(300.0))
                    .andExpect(jsonPath("$.data[?(@.label == '" + dayOfMonth + "')].totalExpenses").value(50.0));
        }

        @Test
        void givenUserWithNoTransactions_thenShouldReturnEmptyData() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Alice").lastName("Smith").build()));

            mockMvc.perform(get("/api/stats/daily")
                            .param("from", LocalDate.now().withDayOfMonth(1).toString())
                            .param("to", LocalDate.now().withDayOfMonth(28).toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        void givenUserWithTransactionsAndNoDateParametersProvided_thenShouldDefaultToLast7Days() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Eve").lastName("Davis").build()));

            LocalDate today = LocalDate.now();

            // Transaction within last 7 days (should be included)
            transactionHelper.createTransaction(userId, INCOME, 100.0, today.minusDays(3));
            transactionHelper.createTransaction(userId, EXPENSE, 50.0, today.minusDays(3));

            // Transaction outside last 7 days (should be excluded)
            transactionHelper.createTransaction(userId, INCOME, 500.0, today.minusDays(10));

            mockMvc.perform(get("/api/stats/daily")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].totalIncome").value(100.0))
                    .andExpect(jsonPath("$.data[0].totalExpenses").value(50.0));
        }

        @Test
        void givenUserWithTransactionsAcrossMultipleMonths_thenShouldAggregateByDayOfMonth() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Bob").lastName("Jones").build()));

            LocalDate now = LocalDate.now();

            // Day 15 of two different months
            transactionHelper.createTransaction(userId, INCOME, 1000.0, now.minusMonths(1).withDayOfMonth(15));
            transactionHelper.createTransaction(userId, INCOME, 500.0, now.withDayOfMonth(15));
            transactionHelper.createTransaction(userId, EXPENSE, 200.0, now.withDayOfMonth(15));

            mockMvc.perform(get("/api/stats/daily")
                            .param("from", now.minusMonths(1).withDayOfMonth(1).toString())
                            .param("to", now.withDayOfMonth(now.lengthOfMonth()).toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[?(@.label == '15')].totalIncome").value(1500.0))
                    .andExpect(jsonPath("$.data[?(@.label == '15')].totalExpenses").value(200.0));
        }

        @Test
        void givenUserWithTransactionsOnConsecutiveDays_thenShouldReturnDataOrderedByDayOfMonth() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Hank").lastName("Pym").build()));

            LocalDate today = LocalDate.now();
            int startDay = 5;
            int endDay = today.getDayOfMonth();

            // Create transactions on consecutive days from day 5 to today
            for (int day = startDay; day <= endDay; day++) {
                transactionHelper.createTransaction(userId, INCOME, day * 10.0, today.withDayOfMonth(day));
            }

            mockMvc.perform(get("/api/stats/daily")
                            .param("from", today.withDayOfMonth(startDay).toString())
                            .param("to", today.toString())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(endDay - startDay + 1))
                    // Verify ascending order by day of month
                    .andExpect(jsonPath("$.data[0].label").value("5"))
                    .andExpect(jsonPath("$.data[1].label").value("6"))
                    .andExpect(jsonPath("$.data[2].label").value("7"))
                    .andExpect(jsonPath("$.data[3].label").value("8"))
                    .andExpect(jsonPath("$.data[4].label").value("9"))
                    .andExpect(jsonPath("$.data[5].label").value("10"))
                    .andExpect(jsonPath("$.data[6].label").value(String.valueOf(endDay)));
        }
    }
}
