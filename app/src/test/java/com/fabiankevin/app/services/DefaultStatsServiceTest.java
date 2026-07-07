package com.fabiankevin.app.services;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

        when(transactionRepository.sumByTypeAndUserId(eq(userId), eq(fromDate), eq(toDate), eq(categoryId)))
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

        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId));
    }

    @Test
    void getStatsSummary_givenNullDates_thenShouldDefaultToCurrentMonth() {
        UUID userId = UUID.randomUUID();

        StatsQuery query = StatsQuery.builder()
                .build();

        double currentIncome = 2000.0;
        double currentExpenses = 1500.0;
        double totalBalance = 10000.0;

        when(transactionRepository.sumByTypeAndUserId(eq(userId), any(), any(), any()))
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

        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(userId));
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenValidUserIdAndDate_thenShouldReturnDailyBalances() {
        UUID userId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 1);

        List<SummaryPoint> dailyBalances = List.of(
                new SummaryPoint("1", 1000.0),
                new SummaryPoint("2", 1500.0),
                new SummaryPoint("3", 1200.0)
        );

        when(transactionRepository.findDailyTotalBalanceByUserIdsAndDateTimeFrom(eq(List.of(userId)), eq(fromDate)))
                .thenReturn(dailyBalances);

        List<SummaryPoint> result = statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(userId, fromDate);

        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should return 3 daily balances");
        assertEquals("1", result.get(0).label(), "First day label should match");
        assertEquals(1000.0, result.get(0).total(), 0.001, "First day total should match");

        verify(transactionRepository, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(eq(List.of(userId)), eq(fromDate));
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenNoTransactions_thenShouldReturnEmptyList() {
        UUID userId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 1);

        when(transactionRepository.findDailyTotalBalanceByUserIdsAndDateTimeFrom(eq(List.of(userId)), eq(fromDate)))
                .thenReturn(List.of());

        List<SummaryPoint> result = statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(userId, fromDate);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when no transactions exist");

        verify(transactionRepository, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(eq(List.of(userId)), eq(fromDate));
    }

    @Test
    void findDailyTotalBalanceByUserIdsAndDateTimeFrom_givenDifferentUserId_thenShouldWrapInSingletonList() {
        UUID userId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 1);

        when(transactionRepository.findDailyTotalBalanceByUserIdsAndDateTimeFrom(eq(List.of(userId)), eq(fromDate)))
                .thenReturn(List.of(new SummaryPoint("5", 500.0)));

        statsService.findDailyTotalBalanceByUserIdsAndDateTimeFrom(userId, fromDate);

        ArgumentCaptor<List<UUID>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository, times(1)).findDailyTotalBalanceByUserIdsAndDateTimeFrom(userIdsCaptor.capture(), eq(fromDate));

        assertEquals(1, userIdsCaptor.getValue().size(), "Should wrap single userId in a list");
        assertEquals(userId, userIdsCaptor.getValue().get(0), "List should contain the expected userId");
    }
}
