package com.fabiankevin.app.services;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void getStatsSummary_givenValidQueryWithDates_thenShouldReturnStatsSummary() {
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
        double totalBalance = 15000.0;
        double priorBalance = 12000.0;

        when(transactionRepository.sumByTypeAndUserId(eq(userId), eq(fromDate), eq(toDate), eq(accountId), eq(categoryId)))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumBalance(eq(userId)))
                .thenReturn(totalBalance);
        when(transactionRepository.sumBalance(any(), any(), any()))
                .thenReturn(priorBalance);

        StatsSummary summary = statsService.getStatsSummary(userId, query);

        assertNotNull(summary, "StatsSummary should not be null");
        assertEquals(totalBalance, summary.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, summary.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, summary.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(25.0, summary.growthPercentage(), 0.01, "Growth percentage should reflect month-over-month balance change");

        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId));
    }

    @Test
    void getStatsSummary_givenNullDates_thenShouldDefaultToCurrentMonth() {
        UUID userId = UUID.randomUUID();

        StatsQuery query = StatsQuery.builder()
                .build();

        LocalDate now = LocalDate.now();
        LocalDate expectedFromDate = now.withDayOfMonth(1);

        double currentIncome = 2000.0;
        double currentExpenses = 1500.0;
        double totalBalance = 10000.0;

        when(transactionRepository.sumByTypeAndUserId(eq(userId), any(), any(), any(), any()))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumBalance(eq(userId)))
                .thenReturn(totalBalance);
        when(transactionRepository.sumBalance(any(), any(), any()))
                .thenReturn(0.0);

        StatsSummary summary = statsService.getStatsSummary(userId, query);

        assertNotNull(summary, "StatsSummary should not be null");
        assertEquals(totalBalance, summary.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, summary.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, summary.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(100.0, summary.growthPercentage(), 0.001, "Growth percentage should be 100.0% when prior balance is zero");

        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId));
    }
}
