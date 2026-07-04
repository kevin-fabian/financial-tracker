package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultStatsServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private DefaultStatsService statsService;

    private List<SummaryPoint> summaryPoints(double income, double expenses) {
        return List.of(
                new SummaryPoint("INCOME", income),
                new SummaryPoint("EXPENSE", expenses)
        );
    }

    @Test
    void getStats_givenValidQueryWithDates_thenShouldReturnStats() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        StatsQuery query = StatsQuery.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .accountId(accountId)
                .categoryId(categoryId)
                .build();

        double currentIncome = 5000.0;
        double currentExpenses = 3000.0;
        double priorIncome = 4000.0;
        double priorExpenses = 2500.0;
        double totalBalance = 15000.0;

        when(transactionRepository.sumByTypeAndUserId(eq(userId), eq(fromDate), eq(toDate), eq(accountId), eq(categoryId)))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumByTypeAndUserId(eq(userId), argThat(d -> d.isBefore(fromDate)), any(), eq(accountId), eq(categoryId)))
                .thenReturn(summaryPoints(priorIncome, priorExpenses));
        when(transactionRepository.sumBalance(eq(userId), eq(accountId)))
                .thenReturn(totalBalance);

        StatsResponse response = statsService.getStats(userId, query);

        assertNotNull(response, "StatsResponse should not be null");
        assertEquals(totalBalance, response.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, response.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, response.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(33.3333, response.growthPercentage(), 0.01, "Growth percentage should reflect prior period comparison");

        verify(transactionRepository, times(2)).sumByTypeAndUserId(any(), any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId), eq(accountId));
    }

    @Test
    void getStats_givenNullDates_thenShouldDefaultToCurrentMonth() {
        UUID userId = UUID.randomUUID();

        StatsQuery query = StatsQuery.builder()
                .build();

        LocalDate now = LocalDate.now();
        LocalDate expectedFromDate = now.withDayOfMonth(1);
        LocalDate expectedToDate = now.plusDays(1);

        double currentIncome = 2000.0;
        double currentExpenses = 1500.0;
        double priorIncome = 0.0;
        double priorExpenses = 0.0;
        double totalBalance = 10000.0;

        when(transactionRepository.sumByTypeAndUserId(eq(userId), eq(expectedFromDate), eq(expectedToDate), any(), any()))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumByTypeAndUserId(eq(userId), argThat(d -> d.isBefore(expectedFromDate)), any(), any(), any()))
                .thenReturn(summaryPoints(priorIncome, priorExpenses));
        when(transactionRepository.sumBalance(eq(userId), any()))
                .thenReturn(totalBalance);

        StatsResponse response = statsService.getStats(userId, query);

        assertNotNull(response, "StatsResponse should not be null");
        assertEquals(totalBalance, response.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, response.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, response.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(100.0, response.growthPercentage(), 0.001, "Growth percentage should be 100.0% when prior net is zero");

        verify(transactionRepository, times(2)).sumByTypeAndUserId(any(), any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId), any());
    }
}
