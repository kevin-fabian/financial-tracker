package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.services.StatsService;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({GlobalExceptionHandler.class})
@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @Autowired
    private JsonMapper jsonMapper;
    private Jwt jwt;

    @BeforeEach
    void setup() {
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
                .subject(UUID.randomUUID().toString())
                .header("alg", "RS256")
                .audience(List.of("financial-tracker-test"))
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void getStats_givenValidParams_thenShouldReturnStats() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        StatsSummary summary = StatsSummary.builder()
                .totalBalance(15000.0)
                .totalExpenses(3000.0)
                .totalIncome(5000.0)
                .growthPercentage(50.0)
                .build();

        when(statsService.getStatsSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/stats")
                        .with(jwt().jwt(jwt))
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("accountId", accountId.toString())
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(15000.0))
                .andExpect(jsonPath("$.totalExpenses").value(3000.0))
                .andExpect(jsonPath("$.totalIncome").value(5000.0))
                .andExpect(jsonPath("$.growthPercentage").value(50.0));

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<StatsQuery> queryCaptor = ArgumentCaptor.forClass(StatsQuery.class);
        verify(statsService, times(1)).getStatsSummary(userIdCaptor.capture(), queryCaptor.capture());

        StatsQuery capturedQuery = queryCaptor.getValue();
        assertEquals(from, capturedQuery.fromDate(), "fromDate should match request param");
        assertEquals(to, capturedQuery.toDate(), "toDate should match request param");
        assertEquals(accountId, capturedQuery.accountId(), "accountId should match request param");
        assertEquals(categoryId, capturedQuery.categoryId(), "categoryId should match request param");
        assertEquals(jwt.getSubject(), userIdCaptor.getValue().toString(), "userId should be extracted from JWT subject");
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenValidFrom_thenShouldReturnDailyBalances() throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);

        List<SummaryPoint> dailyBalances = List.of(
                new SummaryPoint("1", 1000.0),
                new SummaryPoint("2", 1500.0),
                new SummaryPoint("3", 1200.0)
        );

        when(statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(any(), any())).thenReturn(dailyBalances);

        mockMvc.perform(get("/api/stats/daily-balance")
                        .with(jwt().jwt(jwt))
                        .param("from", from.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].label").value("1"))
                .andExpect(jsonPath("$[0].total").value(1000.0))
                .andExpect(jsonPath("$[1].label").value("2"))
                .andExpect(jsonPath("$[1].total").value(1500.0))
                .andExpect(jsonPath("$[2].label").value("3"))
                .andExpect(jsonPath("$[2].total").value(1200.0));

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(statsService, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(userIdCaptor.capture(), dateCaptor.capture());

        assertEquals(jwt.getSubject(), userIdCaptor.getValue().toString(), "userId should be extracted from JWT subject");
        assertEquals(from, dateCaptor.getValue(), "from date should match request param");
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenNoFromParam_thenShouldDefaultToToday() throws Exception {
        LocalDate today = LocalDate.now();

        when(statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(any(), eq(today)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/stats/daily-balance")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(statsService, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(any(), dateCaptor.capture());

        assertEquals(today, dateCaptor.getValue(), "Should default to today when no from param provided");
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenNoTransactions_thenShouldReturnEmptyArray() throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);

        when(statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/stats/daily-balance")
                        .with(jwt().jwt(jwt))
                        .param("from", from.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(statsService, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(any(), eq(from));
    }
}
